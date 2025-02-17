# Infrastructure deployment for Aurora DSQL secret rotation

## Overview

Deploy the below resources

1. Secret stored in AWS Secrets Manager for Amazon Aurora DSQL
2. Secret rotation using AWS Lambda for Amazon Aurora DSQL. The secret rotation is configured for [every 4 hours](./lib/aurora-dsql-secret-rotation-stack.ts#L138).

## Pre-requisites

1. Refer the [README](../README.md) in the top-level directory
2. Export AWS credentials to enable deployment of the resources using AWS CDK

## Deployment

```shell
# Verify you are in the infrastructure folder, else navigate to infrastructure folder
cd infrastructure

# Export the Amazon Aurora DSQL Cluster ID
export DSQL_CLUSTER_ID="<Please provide Aurora DSQL Cluster ID>"
export AWS_DEFAULT_REGION="<Preferred AWS region>"

# Using BuildKit for multi-platform support
# Skip if you have executed this as part of the README in top-level directory
export DOCKER_BUILDKIT=1
docker buildx create --name buildx-builder --use

# Initiate build for AWS Lambda layer
chmod +x build-layer.sh
./build-layer.sh

# Build and deploy CDK stack with optimizations
npm ci --prefer-offline --no-audit --silent # Faster than npm install
npm run build --if-present
npm run test -- --silent
cdk bootstrap
cdk deploy
```

## Cleanup

```shell
cdk destroy
```