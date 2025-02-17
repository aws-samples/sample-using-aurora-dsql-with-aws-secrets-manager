# Demo application using Amazon Aurora DSQL with AWS Secrets Manager

## Overview

This is a Spring Boot application that provides REST APIs for managing demo customer data.

## Technical Stack

- Java
- Spring Boot
- Spring MVC
- JPA/Hibernate
- AWS Secrets Manager
- Amazon Aurora DSQL

## Pre-requisites

1. Refer the [README](../README.md) in the top-level directory
2. Export AWS credentials to enable access to AWS Secrets Manager and Amazon Aurora DSQL

## Build, test and run the application

```shell
# Verify you are in the demo-app folder, else navigate to demo-app folder
cd demo-app

export SPRING_PROFILES_ACTIVE=dev
chmod +x ./gradlew
./gradlew clean build bootRun
```

## Package as a container (Optional)

```shell
# Using BuildKit for multi-platform support
# Skip if you have executed this as part of the README in top-level directory
export DOCKER_BUILDKIT=1
docker buildx create --name buildx-builder --use

# Build the container
docker buildx build --platform linux/amd64 -t demo-app:latest . --load
```

## Testing the APIs

> **Note:** Here we are running the application locally

```shell
# GET all customers - `/api/v1/customers`
curl -s \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  http://localhost:8080/api/v1/customers | jq

# GET customer by id - `/api/v1/customers/{id}`
# **Note:** Replace the placeholder `<UUID>` with the respective ID for the record

curl -s \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  http://localhost:8080/api/v1/customers/<UUID> | jq

# POST Add a customer - `/api/v1/customers`

curl -X POST \
  -s \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Doe",
    "email": "jane.doe@email.com",
    "addresses": [
      {
        "street": "123 Main St",
        "postalCode": "98101",
        "city": "Seattle",
        "state": "WA",
        "country": "USA"
      }
    ]
  }' \
  http://localhost:8080/api/v1/customers | jq

# PUT Update a customer - `/api/v1/customers/{id}`
# **Note:** Replace the placeholder `<UUID>` with the respective ID for the record

curl -X PUT \
  -s \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Doe",
    "email": "jane.doe@email.com",
    "addresses": [
      {
        "street": "123 Main St",
        "postalCode": "98101",
        "city": "Seattle",
        "state": "WA",
        "country": "USA"
      }
    ]
  }' \
  http://localhost:8080/api/v1/customers/<UUID> | jq

# DELETE Delete a customer - `/api/v1/customers/{id}`
# **Note:** Replace the placeholder `<UUID>` with the respective ID for the record

curl -X DELETE \
  -s \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  http://localhost:8080/api/v1/customers/<UUID> | jq
```

## Script for Aurora DSQL CLI access using credentials Secrets Manager
You can use the script [psql.sh](./psql.sh) to open a CLI terminal with Aurora DSQL using the credentials from secrets manager.

1. Export AWS credentials to enable access to AWS Secrets Manager and Amazon Aurora DSQL
2. Run [psql.sh](./psql.sh) in your terminal