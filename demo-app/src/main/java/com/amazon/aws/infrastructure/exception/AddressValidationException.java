// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.infrastructure.exception;

/**
 * Exception thrown when a customer has more than one default address.
 */
public class AddressValidationException extends RuntimeException {
    public AddressValidationException(String message) {
        super(message);
    }
}
