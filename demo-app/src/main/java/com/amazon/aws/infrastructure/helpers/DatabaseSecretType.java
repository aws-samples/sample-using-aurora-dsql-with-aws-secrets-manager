// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.infrastructure.helpers;

import lombok.Getter;

/**
 * Enum representing supported database engines and their JDBC configurations.
 * Contains mapping for JDBC URL prefixes and driver class names.
 */
@Getter
public enum DatabaseSecretType {

    POSTGRES("jdbc:postgresql://", "org.postgresql.Driver", "?sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory");

    private final String jdbcPrefix;
    private final String jdbcDriverClassName;
    private final String sslModeSuffix;

    /**
     * Constructor for DatabaseSecretType
     *
     * @param jdbcPrefix          JDBC URL prefix for the database
     * @param jdbcDriverClassName fully qualified driver class name
     * @param sslModeSuffix       JDBC SSL mode suffix
     */
    DatabaseSecretType(String jdbcPrefix, String jdbcDriverClassName, String sslModeSuffix) {
        this.jdbcPrefix = jdbcPrefix;
        this.jdbcDriverClassName = jdbcDriverClassName;
        this.sslModeSuffix = sslModeSuffix;
    }
}
