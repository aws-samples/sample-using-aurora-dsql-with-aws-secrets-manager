// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.infrastructure.config;


import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.amazon.aws.infrastructure.dto.DatabaseSecret;
import com.amazon.aws.infrastructure.exception.SecretsNotRetrievedException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

/**
 * <p>AWSSecretsManagerConfig is a class that retrieves the secret from AWS Secrets Manager</p>
 * <p>Uses the AppConfig to get the secret key</p>
 * <p>Uses the SecretsManagerClient to get the secret from AWS Secrets Manager</p>
 * <p>Uses the ObjectMapper to map the secret to an DatabaseSecret object</p>
 */
@Slf4j
@Configuration
@Profile("!local")
public class AWSSecretsManagerConfig {

    private final AppConfig appConfig;

    @Autowired
    public AWSSecretsManagerConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public DatabaseSecret getSecret() {
        String secretId = this.appConfig.getSecretKey();
        DatabaseSecret databaseSecret = null;
        
        // Use try-with-resources to ensure proper resource cleanup
        try (SecretsManagerClient client = SecretsManagerClient.builder().build()) {
            // Creating the secret value for the secretId
            GetSecretValueRequest secretValueRequest = GetSecretValueRequest.builder()
                .secretId(secretId)
                .build();
                
            GetSecretValueResponse secretValueResponse;
            try {
                secretValueResponse = client.getSecretValue(secretValueRequest);
            } catch (SecretsManagerException e) {
                log.error("Failed to retrieve secret: {}", e.getMessage());
                throw new SecretsNotRetrievedException("Failed to retrieve secret from AWS Secrets Manager");
            }

            // Initialize secret value holders
            String secret = secretValueResponse.secretString();

            // Validate that we received a secret
            if (secret == null || secret.isEmpty()) {
                log.error("Retrieved secret is null or empty");
                throw new SecretsNotRetrievedException("Retrieved secret is null or empty");
            }

            // Parse the secret JSON
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                databaseSecret = objectMapper.readValue(secret, DatabaseSecret.class);
                log.info("Successfully fetched the secret from AWS Secrets Manager");
                
                // Validate required fields in the secret
                validateDatabaseSecret(databaseSecret);
            } catch (IOException e) {
                log.error("Failed to parse secret JSON: {}", e.getMessage());
                throw new SecretsNotRetrievedException("Failed to parse secret JSON from AWS Secrets Manager");
            }
        }

        return databaseSecret;
    }
    
    /**
     * Validates that the database secret contains all required fields
     * @param secret The database secret to validate
     * @throws SecretsNotRetrievedException if any required field is missing
     */
    private void validateDatabaseSecret(DatabaseSecret secret) {
        if (secret == null) {
            throw new SecretsNotRetrievedException("Database secret is null");
        }
        
        if (secret.username() == null || secret.username().isEmpty()) {
            throw new SecretsNotRetrievedException("Database username is missing in the secret");
        }
        
        if (secret.password() == null || secret.password().isEmpty()) {
            throw new SecretsNotRetrievedException("Database password is missing in the secret");
        }
        
        if (secret.host() == null || secret.host().isEmpty()) {
            throw new SecretsNotRetrievedException("Database host is missing in the secret");
        }
        
        if (secret.engine() == null || secret.engine().isEmpty()) {
            throw new SecretsNotRetrievedException("Database engine is missing in the secret");
        }

        if (secret.port() == null || secret.port().isEmpty()) {
            throw new SecretsNotRetrievedException("Database port is missing in the secret");
        }

        if (secret.dbname() == null || secret.dbname().isEmpty()) {
            throw new SecretsNotRetrievedException("Database name is missing in the secret");
        }
    }
}
