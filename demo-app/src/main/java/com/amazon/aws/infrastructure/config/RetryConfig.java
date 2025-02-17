// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * This class is used to configure the retry template for the application.
 * The retry template is used to retry the operations in case of failures.
 * </p>
 */
@Configuration
@EnableRetry
public class RetryConfig {

    private final AppConfig appConfig;

    public RetryConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Configure token bucket backoff policy
        TokenBucketBackOffPolicy policy = TokenBucketBackOffPolicy.builder()
                .permitsPerSecond(appConfig.getRetryPermitsPerSecond())
                .timeoutDuration(Duration.ofSeconds(appConfig.getRetryTimeoutInSeconds()))
                .initialInterval(appConfig.getInitialRetryDelayInMilliseconds())
                .maxInterval(appConfig.getMaxRetryDelayInMilliseconds())
                .multiplier(appConfig.getRetryBackoffMultiplier())
                .build();

        // Configure retry policy with specific exceptions
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(QueryTimeoutException.class, true);
        retryableExceptions.put(CannotAcquireLockException.class, true);

        // Add SimpleRetryPolicy just for max attempts control
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(this.appConfig.getMaxRetryAttempts(), retryableExceptions);

        // Set the policies
        retryTemplate.setBackOffPolicy(policy);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }
}
