# AWS Console Deployment Guide for Fit4Ever

## Overview
This guide will walk you through deploying your Fit4Ever Spring Boot application to AWS using the AWS Management Console (web interface) instead of CLI commands.

## Architecture
```
Internet → Application Load Balancer → ECS Fargate → RDS PostgreSQL
```

## Prerequisites
- AWS Account with appropriate permissions
- Docker Desktop running locally
- Your application containerized (✅ Already done!)

---

## Step 1: Create Amazon ECR Repository

### What is ECR?
Amazon Elastic Container Registry (ECR) is where we'll store your Docker image.

### Steps:
1. **Login to AWS Console**: Go to [AWS Console](https://console.aws.amazon.com/)
2. **Navigate to ECR**:
   - Search for "ECR" in the services search bar
   - Click on "Elastic Container Registry"
3. **Create Repository**:
   - Click "Create repository"
   - Repository name: `fit4ever`
   - Visibility: Private
   - Leave other settings as default
   - Click "Create repository"
4. **Note the Repository URI**: You'll see something like:
   ```
   123456789012.dkr.ecr.us-east-1.amazonaws.com/fit4ever
   ```

---

## Step 2: Push Docker Image to ECR

### Get AWS CLI Login Command:
1. In your ECR repository, click "View push commands"
2. Copy the login command (first command)

### Push Your Image:
```bash
# 1. Login to ECR (replace with your command from AWS Console)
aws ecr-public get-login-password --region us-east-1 | docker login --username AWS --password-stdin public.ecr.aws/x2s7b6x5

# 2. Tag your image (replace with your ECR URI)
docker tag fit4ever:latest 123456789012.dkr.ecr.us-east-1.amazonaws.com/fit4ever:latest

# 3. Push the image
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/fit4ever:latest
```

---

## Step 3: Create RDS PostgreSQL Database

### Steps:
1. **Navigate to RDS**:
   - Search for "RDS" in the AWS Console
   - Click on "Amazon RDS"

2. **Create Database**:
   - Click "Create database"
   - **Engine type**: PostgreSQL
   - **Engine Version**: Latest (15.x)
   - **Templates**: Free tier (if eligible) or Dev/Test

3. **Settings**:
   - **DB instance identifier**: `fit4ever-db`
   - **Master username**: `fit4ever_user`
   - **Master password**: `Fit4Ever123!` (or your secure password)

4. **Instance Configuration**:
   - **DB instance class**: db.t3.micro (free tier) or db.t4g.micro
   - **Storage**: 20 GB (minimum)

5. **Connectivity**:
   - **VPC**: Default VPC
   - **Public access**: Yes (for initial setup)
   - **VPC security groups**: Create new
   - **Security group name**: `fit4ever-rds-sg`

6. **Additional Configuration**:
   - **Initial database name**: `fit4ever`
   - **Backup retention**: 1 day (minimum)

7. **Click "Create database"**

8. **Wait for creation** (5-10 minutes)

9. **Note the Endpoint**: After creation, note the endpoint URL like:
   ```
   fit4ever-db.xxxxxxxxxx.us-east-1.rds.amazonaws.com
   ```

---

## Step 4: Create ECS Cluster

### Steps:
1. **Navigate to ECS**:
   - Search for "ECS" in AWS Console
   - Click on "Elastic Container Service"

2. **Create Cluster**:
   - Click "Create Cluster"
   - **Cluster name**: `fit4ever-cluster`
   - **Infrastructure**: AWS Fargate (serverless)
   - Leave other settings as default
   - Click "Create"

---

## Step 5: Create Task Definition

### Steps:
1. **In ECS, go to Task Definitions**:
   - Click "Task definitions" in the left sidebar
   - Click "Create new task definition"

2. **Task Definition Configuration**:
   - **Family name**: `fit4ever-task`
   - **Launch type**: Fargate
   - **Operating system**: Linux/X86_64
   - **CPU**: 0.5 vCPU
   - **Memory**: 1 GB

3. **Container Configuration**:
   - **Container name**: `fit4ever-container`
   - **Image URI**: Your ECR image URI (from Step 2)
   - **Port mappings**: 
     - Container port: `8080`
     - Protocol: TCP
     - Port name: `fit4ever-port`

4. **Environment Variables**:
   Add these environment variables:
   ```
   SPRING_PROFILES_ACTIVE = prod
   DATABASE_URL = jdbc:postgresql://YOUR_RDS_ENDPOINT:5432/fit4ever?user=fit4ever_user&password=Fit4Ever123!
   JWT_SECRET = your-super-secret-jwt-key-change-this-in-production-make-it-very-long-and-secure
   ```

5. **Health Check**:
   - **Command**: `/bin/sh -c "curl -f http://localhost:8080/actuator/health || exit 1"`
   - **Interval**: 30 seconds
   - **Timeout**: 5 seconds
   - **Start period**: 60 seconds
   - **Retries**: 3

6. **Click "Create"**

---

## Step 6: Create Application Load Balancer

### Steps:
1. **Navigate to EC2**:
   - Search for "EC2" in AWS Console
   - Go to "Load Balancers" in the left sidebar

2. **Create Load Balancer**:
   - Click "Create Load Balancer"
   - Choose "Application Load Balancer"
   - Click "Create"

3. **Basic Configuration**:
   - **Name**: `fit4ever-alb`
   - **Scheme**: Internet-facing
   - **IP address type**: IPv4

4. **Network Mapping**:
   - **VPC**: Default VPC
   - **Mappings**: Select at least 2 availability zones

5. **Security Groups**:
   - Create new security group or use existing
   - **Allow**: HTTP (port 80) from anywhere (0.0.0.0/0)
   - **Allow**: HTTPS (port 443) from anywhere (optional)

6. **Listeners and Routing**:
   - **Protocol**: HTTP
   - **Port**: 80
   - **Target group**: Create new target group
     - **Target group name**: `fit4ever-targets`
     - **Target type**: IP addresses
     - **Protocol**: HTTP
     - **Port**: 8080
     - **Health check path**: `/actuator/health`

7. **Click "Create load balancer"**

---

## Step 7: Update Security Groups

### RDS Security Group:
1. **Go to EC2 > Security Groups**
2. **Find your RDS security group** (`fit4ever-rds-sg`)
3. **Edit inbound rules**:
   - **Type**: PostgreSQL
   - **Port**: 5432
   - **Source**: Security group of your ECS tasks (will be created automatically)

### ECS Security Group:
1. **Will be created automatically** when you create the service
2. **Should allow**:
   - Port 8080 from the Load Balancer security group

---

## Step 8: Create ECS Service

### Steps:
1. **In your ECS Cluster**:
   - Go to your `fit4ever-cluster`
   - Click "Create Service"

2. **Service Configuration**:
   - **Launch type**: Fargate
   - **Task Definition**: Select `fit4ever-task`
   - **Service name**: `fit4ever-service`
   - **Number of tasks**: 1 (can scale later)

3. **Network Configuration**:
   - **VPC**: Default VPC
   - **Subnets**: Select public subnets
   - **Auto-assign public IP**: Enabled
   - **Security groups**: Create new
     - **Allow**: HTTP traffic on port 8080 from ALB security group

4. **Load Balancer**:
   - **Type**: Application Load Balancer
   - **Load balancer**: Select your `fit4ever-alb`
   - **Target group**: Select `fit4ever-targets`
   - **Container to load balance**: `fit4ever-container:8080`

5. **Click "Create Service"**

---

## Step 9: Test Your Deployment

### Wait for Deployment:
1. **Monitor the service** in ECS console
2. **Wait for tasks to be "RUNNING"** (5-10 minutes)
3. **Check target group health** in EC2 > Target Groups

### Get Your Application URL:
1. **Go to EC2 > Load Balancers**
2. **Find your load balancer**
3. **Copy the DNS name**: something like `fit4ever-alb-xxxxxxxxxx.us-east-1.elb.amazonaws.com`

### Test the Application:
```bash
# Test your application
curl http://your-alb-dns-name.us-east-1.elb.amazonaws.com

# Test API endpoint
curl -X POST http://your-alb-dns-name.us-east-1.elb.amazonaws.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123"}'
```

---

## Step 10: Set Up Custom Domain (Optional)

### If you have a domain:
1. **Route 53**:
   - Create hosted zone for your domain
   - Create A record pointing to your ALB

2. **SSL Certificate**:
   - Request certificate in ACM (Certificate Manager)
   - Add HTTPS listener to your ALB

---

## Troubleshooting

### Common Issues:

1. **Service won't start**:
   - Check CloudWatch logs
   - Verify environment variables
   - Check security groups

2. **Database connection failed**:
   - Verify RDS endpoint in environment variables
   - Check security group rules
   - Ensure RDS is publicly accessible

3. **Load balancer health checks failing**:
   - Verify health check path `/actuator/health`
   - Check if application is running on port 8080
   - Review CloudWatch logs

### Useful Console Locations:
- **ECS Logs**: ECS > Clusters > Tasks > Logs tab
- **CloudWatch Logs**: CloudWatch > Log groups > `/ecs/fit4ever`
- **Target Group Health**: EC2 > Target Groups > Health checks tab

---

## Cost Optimization

### To minimize costs:
1. **Use t3.micro/t4g.micro** for RDS (free tier if eligible)
2. **Use 0.25 vCPU/0.5GB memory** for Fargate tasks
3. **Set up CloudWatch alarms** for cost monitoring
4. **Use Fargate Spot** for development (if needed)

---

## Security Best Practices

1. **Environment Variables**: Store secrets in AWS Systems Manager Parameter Store
2. **VPC**: Move RDS to private subnets in production
3. **WAF**: Add AWS WAF to your ALB for additional security
4. **HTTPS**: Always use HTTPS in production

---

## Scaling

### To handle more traffic:
1. **Auto Scaling**: Set up ECS service auto scaling
2. **Database**: Upgrade RDS instance class
3. **Multi-AZ**: Enable Multi-AZ for RDS high availability

---

## Monitoring

### Set up monitoring:
1. **CloudWatch**: Monitor ECS metrics
2. **Application Insights**: Monitor application performance
3. **Alarms**: Set up alerts for high CPU/memory usage

---

Congratulations! 🎉 Your Fit4Ever application is now running on AWS with a production-ready architecture!
