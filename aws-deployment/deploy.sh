#!/bin/bash

# AWS ECS Deployment Script for Fit4Ever
# Make sure to configure AWS CLI before running this script

set -e

# Configuration (Update these values)
AWS_REGION="us-east-1"
AWS_ACCOUNT_ID="YOUR_ACCOUNT_ID"
ECR_REPOSITORY="fit4ever"
ECS_CLUSTER="fit4ever-cluster"
ECS_SERVICE="fit4ever-service"
TASK_DEFINITION="fit4ever-app"

echo "🚀 Starting deployment of Fit4Ever to AWS ECS..."

# 1. Build and tag Docker image
echo "📦 Building Docker image..."
docker build -t $ECR_REPOSITORY:latest .

# 2. Login to ECR
echo "🔐 Logging in to Amazon ECR..."
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

# 3. Create ECR repository if it doesn't exist
echo "📋 Creating ECR repository if it doesn't exist..."
aws ecr describe-repositories --repository-names $ECR_REPOSITORY --region $AWS_REGION 2>/dev/null || \
aws ecr create-repository --repository-name $ECR_REPOSITORY --region $AWS_REGION

# 4. Tag image for ECR
echo "🏷️  Tagging image for ECR..."
docker tag $ECR_REPOSITORY:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY:latest

# 5. Push image to ECR
echo "⬆️  Pushing image to ECR..."
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY:latest

# 6. Update task definition
echo "📝 Updating ECS task definition..."
# Replace placeholders in task definition
sed -i.bak "s/YOUR_ACCOUNT_ID/$AWS_ACCOUNT_ID/g" aws-deployment/task-definition.json
sed -i.bak "s/YOUR_REGION/$AWS_REGION/g" aws-deployment/task-definition.json

# Register new task definition
aws ecs register-task-definition --cli-input-json file://aws-deployment/task-definition.json --region $AWS_REGION

# 7. Update ECS service
echo "🔄 Updating ECS service..."
aws ecs update-service --cluster $ECS_CLUSTER --service $ECS_SERVICE --task-definition $TASK_DEFINITION --region $AWS_REGION

echo "✅ Deployment completed successfully!"
echo "🌐 Your application will be available shortly at your load balancer URL"
