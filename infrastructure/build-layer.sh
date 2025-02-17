#!/bin/bash

## Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
## SPDX-License-Identifier: MIT-0

# Navigate to the project directory
cd lib/functions/secret-rotation/layer || exit

# Create a fresh directory for dependencies
rm -rf python && mkdir -p python

# Build the image
docker buildx build --platform linux/amd64 -t python-lambda-deps:latest . --load

# Copy the installed packages from the container
docker container create --name python-layer-builder python-lambda-deps:latest
docker cp python-layer-builder:/usr/local/lib/python3.13/site-packages/. ./python/
docker container rm python-layer-builder

# Clean up unnecessary files to reduce layer size
find python -type d -name "__pycache__" -exec rm -rf {} +
find python -type d -name "*.dist-info" -exec rm -rf {} +
find python -type d -name "*.egg-info" -exec rm -rf {} +

# Verify the layer was created successfully
if [ ! -d "./python" ] || [ -z "$(ls -A ./python)" ]; then
    echo "Error: Python dependencies were not copied successfully"
    exit 1
fi
