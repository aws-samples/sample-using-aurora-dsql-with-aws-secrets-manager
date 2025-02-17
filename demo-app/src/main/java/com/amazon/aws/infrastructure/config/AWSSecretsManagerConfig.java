// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.infrastructure.config;


import com.amazon.aws.infrastructure.dto.DatabaseSecret;
import com.amazon.aws.infrastructure.exception.SecretsNotRetrievedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.io.IOException;

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
        SecretsManagerClient client = SecretsManagerClient.builder().build();

        // Creating the secret value for the secretId
        GetSecretValueRequest secretValueRequest = GetSecretValueRequest.builder().secretId(secretId).build();
        GetSecretValueResponse secretValueResponse = client.getSecretValue(secretValueRequest);

        // Initialize secret value holders
        String secret = null;

        // Decrypts secret using the associated KMS CMK.
        // Depending on whether the secret is a string or binary, one of these fields will be populated.
        if (secretValueResponse.secretString() != null) {
            secret = secretValueResponse.secretString();
        }

        // ==================================================================
        if (null != secret) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                databaseSecret = objectMapper.readValue(secret, DatabaseSecret.class);
                log.info("Fetched the secret from AWS secrets manager");
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new SecretsNotRetrievedException(e.getMessage());
            }
        }
        // ==================================================================

        return databaseSecret;
    }

}