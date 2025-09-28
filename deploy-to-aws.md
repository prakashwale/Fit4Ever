# Deploy Fit4Ever to AWS - Step by Step Guide

## 🚀 **Method 1: AWS ECS with ECR (Docker-based) - RECOMMENDED**

### **Step 1: Set up AWS CLI and ECR**

```bash
# Install AWS CLI if not already installed
aws configure

# Create ECR repository
aws ecr create-repository --repository-name fit4ever --region us-east-1

# Get login token for ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <your-account-id>.dkr.ecr.us-east-1.amazonaws.com
```

### **Step 2: Build and Push Docker Image**

```bash
# Build the Docker image
docker build -t fit4ever .

# Tag the image for ECR
docker tag fit4ever:latest <your-account-id>.dkr.ecr.us-east-1.amazonaws.com/fit4ever:latest

# Push to ECR
docker push <your-account-id>.dkr.ecr.us-east-1.amazonaws.com/fit4ever:latest
```

### **Step 3: Set up RDS PostgreSQL Database**

```bash
# Create RDS PostgreSQL instance
aws rds create-db-instance \
    --db-instance-identifier fit4ever-db \
    --db-instance-class db.t3.micro \
    --engine postgres \
    --master-username fit4ever_user \
    --master-user-password "YourSecurePassword123!" \
    --allocated-storage 20 \
    --vpc-security-group-ids sg-xxxxxxxx \
    --db-name fit4ever \
    --port 5432 \
    --backup-retention-period 7 \
    --storage-encrypted
```

### **Step 4: Create ECS Task Definition**

The `aws-deployment/task-definition.json` is already created. Update it with your values:

```json
{
  "family": "fit4ever-task",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::<account-id>:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "fit4ever-container",
      "image": "<account-id>.dkr.ecr.us-east-1.amazonaws.com/fit4ever:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "prod"
        },
        {
          "name": "DATABASE_URL",
          "value": "jdbc:postgresql://your-rds-endpoint:5432/fit4ever?user=fit4ever_user&password=YourSecurePassword123!"
        },
        {
          "name": "JWT_SECRET",
          "value": "your-super-secure-jwt-secret-key-for-production-use-256-bit-key"
        },
        {
          "name": "GOOGLE_CLIENT_ID",
          "value": "your-google-client-id"
        },
        {
          "name": "GOOGLE_CLIENT_SECRET",
          "value": "your-google-client-secret"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/fit4ever",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

### **Step 5: Deploy to ECS**

```bash
# Register task definition
aws ecs register-task-definition --cli-input-json file://aws-deployment/task-definition.json

# Create ECS cluster
aws ecs create-cluster --cluster-name fit4ever-cluster

# Create ECS service
aws ecs create-service \
    --cluster fit4ever-cluster \
    --service-name fit4ever-service \
    --task-definition fit4ever-task:1 \
    --desired-count 1 \
    --launch-type FARGATE \
    --network-configuration "awsvpcConfiguration={subnets=[subnet-xxxxxxxx],securityGroups=[sg-xxxxxxxx],assignPublicIp=ENABLED}"
```

---

## 🚀 **Method 2: AWS App Runner (Easiest)**

### **Step 1: Create apprunner.yaml**

```yaml
version: 1.0
runtime: docker
build:
  commands:
    build:
      - echo "Building Docker image"
run:
  runtime-version: latest
  command: java -jar app.jar
  network:
    port: 8080
    env: PORT
  env:
    - name: SPRING_PROFILES_ACTIVE
      value: prod
    - name: DATABASE_URL
      value: jdbc:postgresql://your-rds-endpoint:5432/fit4ever
    - name: JWT_SECRET
      value: your-jwt-secret
    - name: GOOGLE_CLIENT_ID
      value: your-google-client-id
    - name: GOOGLE_CLIENT_SECRET
      value: your-google-client-secret
```

### **Step 2: Deploy via AWS Console**

1. Go to AWS App Runner console
2. Create new service
3. Connect to your GitHub repository
4. Select automatic deployment
5. Configure environment variables
6. Deploy!

---

## 🚀 **Method 3: AWS Elastic Beanstalk (Java-based)**

### **Step 1: Install EB CLI**

```bash
pip install awsebcli
```

### **Step 2: Initialize and Deploy**

```bash
# Initialize Elastic Beanstalk
eb init fit4ever --platform java-17 --region us-east-1

# Create environment
eb create fit4ever-prod

# Set environment variables
eb setenv SPRING_PROFILES_ACTIVE=prod \
          DATABASE_URL="jdbc:postgresql://your-rds-endpoint:5432/fit4ever" \
          JWT_SECRET="your-jwt-secret" \
          GOOGLE_CLIENT_ID="your-google-client-id" \
          GOOGLE_CLIENT_SECRET="your-google-client-secret"

# Deploy
eb deploy
```

---

## 🔄 **Auto-Deployment with GitHub Actions**

Create `.github/workflows/deploy.yml`:

```yaml
name: Deploy to AWS

on:
  push:
    branches: [ main ]

jobs:
  deploy:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Configure AWS credentials
      uses: aws-actions/configure-aws-credentials@v2
      with:
        aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
        aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        aws-region: us-east-1
    
    - name: Login to Amazon ECR
      id: login-ecr
      uses: aws-actions/amazon-ecr-login@v1
    
    - name: Build, tag, and push image to Amazon ECR
      env:
        ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
        ECR_REPOSITORY: fit4ever
        IMAGE_TAG: ${{ github.sha }}
      run: |
        docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG .
        docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
    
    - name: Update ECS service
      run: |
        aws ecs update-service --cluster fit4ever-cluster --service fit4ever-service --force-new-deployment
```

---

## 📋 **Quick Update Commands**

### **For ECS Deployment:**
```bash
# Build and push new image
docker build -t fit4ever .
docker tag fit4ever:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/fit4ever:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/fit4ever:latest

# Force service update
aws ecs update-service --cluster fit4ever-cluster --service fit4ever-service --force-new-deployment
```

### **For App Runner:**
```bash
# Just push to Git - auto-deploys!
git push origin main
```

### **For Elastic Beanstalk:**
```bash
# Deploy latest version
eb deploy
```

---

## 🔒 **Security Checklist for AWS Deployment**

1. **Environment Variables:**
   - Set strong JWT_SECRET (256-bit)
   - Configure Google OAuth2 credentials
   - Set secure database credentials

2. **Google OAuth2 Setup:**
   - Add AWS domain to authorized redirect URIs
   - Example: `https://your-aws-domain.com/login/oauth2/code/google`

3. **Database Security:**
   - Use RDS with encryption
   - Configure security groups properly
   - Use strong passwords

4. **Network Security:**
   - Configure VPC and subnets
   - Set up proper security groups
   - Use HTTPS with SSL certificate

---

## 🎯 **Recommended Approach**

For your Fit4Ever application, I recommend **AWS App Runner** for the following reasons:

1. **Simplest setup** - connects directly to GitHub
2. **Auto-scaling** - handles traffic automatically
3. **Automatic deployments** - updates on every Git push
4. **Cost-effective** - pay only for what you use
5. **Built-in load balancing** and HTTPS

Choose App Runner for ease, ECS for control, or Elastic Beanstalk for Java familiarity!
