#!/bin/bash

# Quick deployment script for development changes
# Usage: ./scripts/quick-deploy.sh [environment]

set -e

ENVIRONMENT=${1:-dev}
AWS_REGION="us-east-1"
AWS_ACCOUNT_ID="YOUR_ACCOUNT_ID"
ECR_REPOSITORY="fit4ever"
ECS_CLUSTER="fit4ever-cluster"
ECS_SERVICE="fit4ever-service"

echo "🚀 Quick deployment to $ENVIRONMENT environment..."

# Check if we're in the right directory
if [ ! -f "pom.xml" ]; then
    echo "❌ Please run this script from the project root directory"
    exit 1
fi

# Build only if source code changed
if [ "$ENVIRONMENT" = "dev" ]; then
    echo "🔧 Development deployment - using cached layers..."
    docker build -f Dockerfile.dev -t $ECR_REPOSITORY:dev .
else
    echo "🏭 Production deployment - full build..."
    docker build -t $ECR_REPOSITORY:latest .
fi

# Tag with timestamp for unique versions
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
docker tag $ECR_REPOSITORY:$ENVIRONMENT $ECR_REPOSITORY:$TIMESTAMP

# Login to ECR
echo "🔐 Logging in to Amazon ECR..."
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

# Push both tags
echo "⬆️  Pushing images to ECR..."
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY:$ENVIRONMENT
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY:$TIMESTAMP

# Update ECS service
echo "🔄 Updating ECS service..."
aws ecs update-service \
    --cluster $ECS_CLUSTER \
    --service $ECS_SERVICE \
    --force-new-deployment \
    --region $AWS_REGION

echo "✅ Quick deployment completed!"
echo "🌐 Your application will be available shortly"
