// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.infrastructure.helpers;

import org.springframework.retry.RetryContext;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class with helper methods
 */
public final class Util {

    private static final String NAME_PATTERN = "User-%s-%d";
    private static final int UUID_LENGTH = 8;
    private static final int MIN_RANDOM = 1;
    private static final int MAX_RANDOM = 1000;

    private Util() {
        // Prevent instantiation of utility class
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Generates a random name in the format "User-{UUID}-{number}"
     * Thread-safe implementation using ThreadLocalRandom
     *
     * @return A randomly generated name
     */
    public static String generateRandomName() {
        String uniqueId = generateShortUUID();
        int randomNumber = generateRandomNumber();
        return String.format(NAME_PATTERN, uniqueId, randomNumber);
    }

    /**
     * <p>
     * Handles the retry failure by throwing a RuntimeException with the appropriate error message.
     * </p>
     *
     * @param context - The RetryContext object containing information about the retry operation
     * @return A RuntimeException with the appropriate error message
     */
    public static RuntimeException handleRetryFailure(RetryContext context) {
        Throwable lastError = context.getLastThrowable();
        if (lastError == null) {
            return new RuntimeException("Retry operation failed without an error context");
        }
        if (lastError instanceof RuntimeException runtimeEx) {
            return runtimeEx;
        }
        return new RuntimeException("Retry operation failed", lastError);
    }

    /**
     * Generates a shortened UUID
     *
     * @return First 8 characters of a UUID
     */
    private static String generateShortUUID() {
        return UUID.randomUUID().toString().substring(0, UUID_LENGTH);
    }

    /**
     * Generates a random number between MIN_RANDOM and MAX_RANDOM
     *
     * @return Random integer
     */
    private static int generateRandomNumber() {
        return ThreadLocalRandom.current().nextInt(MIN_RANDOM, MAX_RANDOM);
    }
}
