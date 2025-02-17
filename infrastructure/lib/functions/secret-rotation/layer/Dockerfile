# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: MIT-0

ARG PLATFORM=linux/amd64
FROM --platform=${PLATFORM} public.ecr.aws/docker/library/python:3.13-slim

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app
# Copy only requirements first to leverage cache
COPY requirements.txt .

# Combine pip commands and cleanup in single RUN to reduce layers
# Add --no-cache-dir to reduce image size
# Add --upgrade pip for security
# Add pip config to target platform
RUN python -m pip install --upgrade pip && \
    pip config set global.target_platform linux_x86_64 && \
    pip install --no-cache-dir -r requirements.txt && \
    rm -rf /root/.cache/pip

# Set Python to not write bytecode files and not buffer output
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

# Set secure permissions
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser
