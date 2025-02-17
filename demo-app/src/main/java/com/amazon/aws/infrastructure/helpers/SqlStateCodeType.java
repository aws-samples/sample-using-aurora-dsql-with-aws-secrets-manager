// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.infrastructure.helpers;

import lombok.Getter;

import java.util.regex.Pattern;

/**
 * This enum represents the SQL state codes for database errors.
 * It provides a way to check if a given SQL state code indicates a retryable condition.
 */
@Getter
public enum SqlStateCodeType {
    OCC_0("0C000"),
    OCC_1("0C001");

    private static final Pattern OCC_OTHERS = Pattern.compile("0A\\d{3}");
    private final String code;

    SqlStateCodeType(String code) {
        this.code = code;
    }

    /**
     * Checks if the SQL state indicates a retryable condition
     *
     * @param sqlState the SQL state code to check
     * @return true if the SQL state is retryable, false otherwise
     */
    public static boolean isRetryable(String sqlState) {
        if (sqlState == null) {
            return false;
        }

        return OCC_0.getCode().equalsIgnoreCase(sqlState) ||
                OCC_1.getCode().equalsIgnoreCase(sqlState) ||
                OCC_OTHERS.matcher(sqlState).matches();
    }
}