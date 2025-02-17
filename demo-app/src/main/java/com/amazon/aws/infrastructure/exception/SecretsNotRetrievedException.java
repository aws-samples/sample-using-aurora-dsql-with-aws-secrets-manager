// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.infrastructure.exception;

/**
 * Exception thrown when the secret could not be retrieved from AWS Secrets Manager
 */
public class SecretsNotRetrievedException extends RuntimeException {
    public SecretsNotRetrievedException(String message) {
        super(message);
    }
}