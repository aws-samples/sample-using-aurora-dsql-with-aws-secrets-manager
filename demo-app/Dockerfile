# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: MIT-0

# Use Amazon Corretto JDK 17 as the base image
ARG PLATFORM=linux/arm64
FROM --platform=${PLATFORM} public.ecr.aws/amazoncorretto/amazoncorretto:17.0.14 AS builder

# Set working directory
WORKDIR /app

# Copy the Gradle files first
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle

# Download dependencies
RUN ./gradlew dependencies --no-daemon

# Copy the source code
COPY src ./src

# Build the application
# FIXME: Skip the tests since it needs the database to be available. Pending TestContainers integration.
RUN ./gradlew build -x test --no-daemon

# Runtime stage
FROM --platform=${PLATFORM} public.ecr.aws/amazoncorretto/amazoncorretto:17.0.14

# Create non-root user
RUN yum install -y shadow-utils && \
    groupadd -r appuser && \
    useradd -r -g appuser -s /bin/false appuser && \
    mkdir /app && \
    chown -R appuser:appuser /app && \
    yum clean all

WORKDIR /app

# Copy the built artifact from builder stage
COPY --from=builder --chown=appuser:appuser /app/build/libs/demo-app-1.0.jar app.jar

# Configure JVM security options
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 \
    -Djava.security.egd=file:/dev/urandom \
    -Dcom.sun.jndi.ldap.object.trustURLCodebase=false \
    -Dlog4j2.formatMsgNoLookups=true"

# Set Spring profile
ENV SPRING_PROFILES_ACTIVE=dev

# Switch to non-root user
USER appuser

# Expose the port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
