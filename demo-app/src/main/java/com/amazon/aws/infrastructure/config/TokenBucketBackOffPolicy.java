// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.infrastructure.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.BackOffContext;
import org.springframework.retry.backoff.BackOffInterruptedException;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;

import java.time.Duration;
import java.util.Objects;

/**
 * TokenBucketBackOffPolicy is a custom implementation of the BackOffPolicy interface
 * that uses a token bucket algorithm to control the rate of retry attempts.
 * It is designed to be thread-safe and can be configured with various parameters.
 */
@Slf4j
public class TokenBucketBackOffPolicy implements BackOffPolicy {
    private static final int DEFAULT_LIMIT_FOR_PERIOD = 1;
    private static final double MILLISECONDS_IN_SECOND = 1000.0;
    private static final String RATE_LIMITER_NAME = "retry-rate-limiter";
    private static final RateLimiterRegistry REGISTRY = RateLimiterRegistry.ofDefaults();

    private final RateLimiter rateLimiter;
    private final ExponentialRandomBackOffPolicy defaultBackOff;

    private TokenBucketBackOffPolicy(Builder builder) {
        this.rateLimiter = createRateLimiter(builder.permitsPerSecond, builder.timeoutDuration);
        this.defaultBackOff = new ExponentialRandomBackOffPolicy();

        if (builder.initialInterval > 0) {
            defaultBackOff.setInitialInterval(builder.initialInterval);
        }
        if (builder.maxInterval > 0) {
            defaultBackOff.setMaxInterval(builder.maxInterval);
        }
        if (builder.multiplier > 0) {
            defaultBackOff.setMultiplier(builder.multiplier);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private RateLimiter createRateLimiter(double permitsPerSecond, Duration timeoutDuration) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(DEFAULT_LIMIT_FOR_PERIOD)
                .limitRefreshPeriod(Duration.ofMillis((long) (MILLISECONDS_IN_SECOND / permitsPerSecond)))
                .timeoutDuration(timeoutDuration)
                .build();

        return REGISTRY.rateLimiter(RATE_LIMITER_NAME, config);
    }

    @Override
    public BackOffContext start(RetryContext context) {
        Objects.requireNonNull(context, "RetryContext must not be null");
        return defaultBackOff.start(context);
    }

    @Override
    public void backOff(BackOffContext backOffContext) throws BackOffInterruptedException {
        Objects.requireNonNull(backOffContext, "BackOffContext must not be null");

        try {
            if (!rateLimiter.acquirePermission()) {
                log.debug("Rate limit exceeded for {}", RATE_LIMITER_NAME);
                throw new BackOffInterruptedException("Rate limit exceeded");
            }
            defaultBackOff.backOff(backOffContext);
        } catch (io.github.resilience4j.ratelimiter.RequestNotPermitted e) {
            log.debug("Rate limit exceeded for {}: {}", RATE_LIMITER_NAME, e.getMessage());
            throw new BackOffInterruptedException("Rate limit exceeded", e);
        }
    }

    public static class Builder {
        private double permitsPerSecond;
        private Duration timeoutDuration;
        private long initialInterval;
        private long maxInterval;
        private double multiplier;

        private Builder() {
            // Private constructor to enforce the builder pattern
        }

        public Builder permitsPerSecond(double permitsPerSecond) {
            validatePositive(permitsPerSecond, "permitsPerSecond");
            this.permitsPerSecond = permitsPerSecond;
            return this;
        }

        public Builder timeoutDuration(Duration timeoutDuration) {
            this.timeoutDuration = Objects.requireNonNull(timeoutDuration, "timeoutDuration must not be null");
            return this;
        }

        public Builder initialInterval(long initialInterval) {
            validatePositive(initialInterval, "initialInterval");
            this.initialInterval = initialInterval;
            return this;
        }

        public Builder maxInterval(long maxInterval) {
            validatePositive(maxInterval, "maxInterval");
            this.maxInterval = maxInterval;
            return this;
        }

        public Builder multiplier(double multiplier) {
            validatePositive(multiplier, "multiplier");
            this.multiplier = multiplier;
            return this;
        }

        private void validatePositive(double value, String paramName) {
            if (value <= 0) {
                throw new IllegalArgumentException(paramName + " must be positive");
            }
        }

        public TokenBucketBackOffPolicy build() {
            validate();
            return new TokenBucketBackOffPolicy(this);
        }

        private void validate() {
            if (permitsPerSecond <= 0) {
                throw new IllegalStateException("permitsPerSecond must be set and positive");
            }
            if (timeoutDuration == null) {
                throw new IllegalStateException("timeoutDuration must be set");
            }
            if (maxInterval > 0 && initialInterval > maxInterval) {
                throw new IllegalStateException("initialInterval cannot be greater than maxInterval");
            }
        }
    }
}
