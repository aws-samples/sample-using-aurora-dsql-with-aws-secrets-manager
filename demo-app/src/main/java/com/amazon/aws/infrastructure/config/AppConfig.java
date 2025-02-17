// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * <p>Reads the custom configurations with prefix "app" in the application.yml</p>
 * <p>For example, the property "app.secret-key" will be mapped to the field "secretKey"</p>
 */
@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppConfig {

    // AWS Secrets Manager Name
    private String secretKey;

    // JPA Transaction Timeouts
    private Integer transactionTimeoutInSeconds;
    private Long transactionTimeoutInMilliseconds;

    // Server Timeouts
    private Integer serverTimeoutInMilliseconds;

    // Retry configuration
    private Long initialRetryDelayInMilliseconds;
    private Long maxRetryDelayInMilliseconds;
    private Integer maxRetryAttempts;
    private Double retryBackoffMultiplier;
    private Double retryPermitsPerSecond;
    private Integer retryTimeoutInSeconds;
}