#!/bin/bash

# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: MIT-0

# Set error handling
set -euo pipefail

# Define constants
SECRET_ID="/aws/aurora-dsql/secret"
REGION="us-east-1"

# Function to handle errors
error_handler() {
    echo "Error occurred in script at line: ${1}" >&2
    exit 1
}

# Set up error trap
trap 'error_handler ${LINENO}' ERR

# Fetch secret once and store in variable
echo "Fetching database credentials..."
SECRET_STRING=$(aws secretsmanager get-secret-value \
    --secret-id "${SECRET_ID}" \
    --region "${REGION}" \
    --query 'SecretString' \
    --output text) || {
        echo "Failed to fetch secrets from AWS Secrets Manager" >&2
        exit 1
    }

# Extract credentials from secret string
PGPASSWORD=$(echo "${SECRET_STRING}" | jq -r '.password')
HOST=$(echo "${SECRET_STRING}" | jq -r '.host')

# Validate credentials were retrieved
if [[ -z "${PGPASSWORD}" || -z "${HOST}" ]]; then
    echo "Failed to extract database credentials from secret" >&2
    exit 1
fi

# Export necessary environment variables
export PGPASSWORD
export PGSSLMODE=require

# Connect to database
echo "Connecting to database..."
psql --host "${HOST}" \
     --username admin \
     --dbname postgres \
     --quiet \
     -w || {
        echo "Failed to connect to database" >&2
        exit 1
     }
