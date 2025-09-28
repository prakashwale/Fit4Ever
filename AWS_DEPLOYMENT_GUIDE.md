# AWS Deployment Guide for Fit4Ever

## Overview
This guide will help you deploy your Fit4Ever Spring Boot application to AWS using ECS Fargate with PostgreSQL RDS.

## Prerequisites
- AWS Account with appropriate permissions
- AWS CLI installed and configured
- Docker installed locally
- Domain name (optional, for custom domain)

## Architecture
```
Internet → ALB → ECS Fargate → RDS PostgreSQL
```

## Step-by-Step Deployment

### 1. Setup AWS CLI
```bash
# Install AWS CLI v2
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Configure AWS CLI
aws configure
# Enter your Access Key ID, Secret Access Key, Region (e.g., us-east-1), and output format (json)
```

### 2. Create RDS PostgreSQL Database

#### Option A: AWS Console
1. Go to RDS Console
2. Click "Create database"
3. Choose PostgreSQL
4. Select "Free tier" template
5. Configure:
   - DB instance identifier: `fit4ever-db`
   - Master username: `fit4ever_user`
   - Master password: `[secure-password]`
   - DB name: `fit4ever`
6. In "Connectivity", ensure "Public access" is "Yes" for initial setup
7. Create database

#### Option B: AWS CLI
```bash
aws rds create-db-instance \
    --db-instance-identifier fit4ever-db \
    --db-instance-class db.t3.micro \
    --engine postgres \
    --master-username fit4ever_user \
    --master-user-password YOUR_SECURE_PASSWORD \
    --allocated-storage 20 \
    --db-name fit4ever \
    --publicly-accessible \
    --region us-east-1
```

### 3. Store Secrets in AWS Systems Manager

```bash
# Store database URL
aws ssm put-parameter \
    --name "/fit4ever/database-url" \
    --value "jdbc:postgresql://fit4ever-db.XXXXXXXXXX.us-east-1.rds.amazonaws.com:5432/fit4ever?user=fit4ever_user&password=YOUR_SECURE_PASSWORD" \
    --type "SecureString" \
    --region us-east-1

# Store JWT secret
aws ssm put-parameter \
    --name "/fit4ever/jwt-secret" \
    --value "your-super-secret-jwt-key-change-this-in-production-make-it-very-long-and-secure-at-least-64-characters" \
    --type "SecureString" \
    --region us-east-1
```

### 4. Create ECS Cluster

```bash
# Create ECS cluster
aws ecs create-cluster \
    --cluster-name fit4ever-cluster \
    --capacity-providers FARGATE \
    --default-capacity-provider-strategy capacityProvider=FARGATE,weight=1 \
    --region us-east-1
```

### 5. Create IAM Roles

#### ECS Task Execution Role
```bash
# Create trust policy file
cat > ecs-task-execution-trust-policy.json << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ecs-tasks.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF

# Create role
aws iam create-role \
    --role-name ecsTaskExecutionRole \
    --assume-role-policy-document file://ecs-task-execution-trust-policy.json

# Attach policies
aws iam attach-role-policy \
    --role-name ecsTaskExecutionRole \
    --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy

aws iam attach-role-policy \
    --role-name ecsTaskExecutionRole \
    --policy-arn arn:aws:iam::aws:policy/AmazonSSMReadOnlyAccess
```

#### ECS Task Role
```bash
# Create task role
aws iam create-role \
    --role-name ecsTaskRole \
    --assume-role-policy-document file://ecs-task-execution-trust-policy.json

# Attach CloudWatch logs policy
aws iam attach-role-policy \
    --role-name ecsTaskRole \
    --policy-arn arn:aws:iam::aws:policy/CloudWatchLogsFullAccess
```

### 6. Create CloudWatch Log Group

```bash
aws logs create-log-group \
    --log-group-name /ecs/fit4ever \
    --region us-east-1
```

### 7. Create Application Load Balancer

```bash
# Get default VPC ID
VPC_ID=$(aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --query "Vpcs[0].VpcId" --output text)

# Get subnet IDs
SUBNET_IDS=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" --query "Subnets[*].SubnetId" --output text)

# Create security group for ALB
ALB_SG_ID=$(aws ec2 create-security-group \
    --group-name fit4ever-alb-sg \
    --description "Security group for Fit4Ever ALB" \
    --vpc-id $VPC_ID \
    --query "GroupId" --output text)

# Allow HTTP and HTTPS traffic
aws ec2 authorize-security-group-ingress \
    --group-id $ALB_SG_ID \
    --protocol tcp \
    --port 80 \
    --cidr 0.0.0.0/0

aws ec2 authorize-security-group-ingress \
    --group-id $ALB_SG_ID \
    --protocol tcp \
    --port 443 \
    --cidr 0.0.0.0/0

# Create ALB
ALB_ARN=$(aws elbv2 create-load-balancer \
    --name fit4ever-alb \
    --subnets $SUBNET_IDS \
    --security-groups $ALB_SG_ID \
    --query "LoadBalancers[0].LoadBalancerArn" --output text)

# Create target group
TG_ARN=$(aws elbv2 create-target-group \
    --name fit4ever-tg \
    --protocol HTTP \
    --port 8080 \
    --vpc-id $VPC_ID \
    --target-type ip \
    --health-check-path /actuator/health \
    --health-check-interval-seconds 30 \
    --health-check-timeout-seconds 5 \
    --healthy-threshold-count 2 \
    --unhealthy-threshold-count 5 \
    --query "TargetGroups[0].TargetGroupArn" --output text)

# Create listener
aws elbv2 create-listener \
    --load-balancer-arn $ALB_ARN \
    --protocol HTTP \
    --port 80 \
    --default-actions Type=forward,TargetGroupArn=$TG_ARN
```

### 8. Create Security Group for ECS

```bash
# Create security group for ECS tasks
ECS_SG_ID=$(aws ec2 create-security-group \
    --group-name fit4ever-ecs-sg \
    --description "Security group for Fit4Ever ECS tasks" \
    --vpc-id $VPC_ID \
    --query "GroupId" --output text)

# Allow traffic from ALB
aws ec2 authorize-security-group-ingress \
    --group-id $ECS_SG_ID \
    --protocol tcp \
    --port 8080 \
    --source-group $ALB_SG_ID
```

### 9. Deploy Application

1. Update the configuration in `aws-deployment/deploy.sh`:
   ```bash
   # Get your AWS account ID
   AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
   
   # Update deploy.sh with your account ID
   sed -i "s/YOUR_ACCOUNT_ID/$AWS_ACCOUNT_ID/g" aws-deployment/deploy.sh
   ```

2. Update `aws-deployment/task-definition.json` with your account ID and region.

3. Run the deployment script:
   ```bash
   ./aws-deployment/deploy.sh
   ```

### 10. Create ECS Service

```bash
# Get subnet IDs for private subnets (or public if no private subnets)
SUBNET_IDS=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" --query "Subnets[0:2].SubnetId" --output text | tr '\t' ',')

# Create ECS service
aws ecs create-service \
    --cluster fit4ever-cluster \
    --service-name fit4ever-service \
    --task-definition fit4ever-app \
    --desired-count 1 \
    --launch-type FARGATE \
    --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_IDS],securityGroups=[$ECS_SG_ID],assignPublicIp=ENABLED}" \
    --load-balancers targetGroupArn=$TG_ARN,containerName=fit4ever-container,containerPort=8080 \
    --region us-east-1
```

### 11. Get Application URL

```bash
# Get ALB DNS name
ALB_DNS=$(aws elbv2 describe-load-balancers \
    --load-balancer-arns $ALB_ARN \
    --query "LoadBalancers[0].DNSName" --output text)

echo "Your application is available at: http://$ALB_DNS"
```

## Testing the Deployment

1. **Local Testing with Docker Compose:**
   ```bash
   docker-compose up --build
   ```
   Application available at: http://localhost:8080

2. **Test API Endpoints:**
   - Health Check: `GET /actuator/health`
   - Swagger UI: `GET /swagger-ui.html`
   - Register: `POST /api/auth/register`
   - Login: `POST /api/auth/login`

## Monitoring and Logs

- **CloudWatch Logs**: Check `/ecs/fit4ever` log group
- **ECS Console**: Monitor service health and task status
- **RDS Monitoring**: Check database performance metrics

## Cost Optimization

1. **RDS**: Use `db.t3.micro` for free tier
2. **ECS**: Use Fargate Spot for non-production
3. **ALB**: Consider using Network Load Balancer for lower costs
4. **CloudWatch**: Set up log retention policies

## Security Best Practices

1. **Environment Variables**: Use AWS Systems Manager Parameter Store
2. **Database**: Enable encryption at rest
3. **Network**: Use private subnets for ECS tasks
4. **IAM**: Follow principle of least privilege
5. **SSL/TLS**: Add HTTPS listener with ACM certificate

## Troubleshooting

### Common Issues:

1. **Service fails to start**: Check CloudWatch logs
2. **Health checks failing**: Verify health check endpoint
3. **Database connection issues**: Check security groups and RDS accessibility
4. **Task definition registration fails**: Verify IAM roles

### Debug Commands:

```bash
# Check service status
aws ecs describe-services --cluster fit4ever-cluster --services fit4ever-service

# Check task logs
aws logs tail /ecs/fit4ever --follow

# Check task health
aws ecs describe-tasks --cluster fit4ever-cluster --tasks TASK_ID
```

## Cleanup (if needed)

```bash
# Delete ECS service
aws ecs update-service --cluster fit4ever-cluster --service fit4ever-service --desired-count 0
aws ecs delete-service --cluster fit4ever-cluster --service fit4ever-service

# Delete other resources
aws ecs delete-cluster --cluster fit4ever-cluster
aws rds delete-db-instance --db-instance-identifier fit4ever-db --skip-final-snapshot
aws elbv2 delete-load-balancer --load-balancer-arn $ALB_ARN
# ... (continue with other resources)
```

## Next Steps

1. **Custom Domain**: Set up Route 53 with your domain
2. **HTTPS**: Add SSL certificate with AWS Certificate Manager
3. **CI/CD**: Set up GitHub Actions or AWS CodePipeline
4. **Monitoring**: Add CloudWatch alarms and SNS notifications
5. **Backup**: Configure RDS automated backups
