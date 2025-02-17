// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.infrastructure.dto;

import com.amazon.aws.infrastructure.helpers.DatabaseSecretType;

/**
 * <p>
 * DatabaseSecret is a record class that represents an database secret.
 * It contains information about the username, password, engine, host, port, database name, and database instance identifier.
 * </p>
 *
 * @param username
 * @param password
 * @param engine
 * @param host
 * @param port
 * @param dbname
 * @param dbInstanceIdentifier
 */
public record DatabaseSecret(String username, String password, String engine, String host, String port, String dbname,
                             String dbInstanceIdentifier) {

    private String getJdbcPrefix() {
        return DatabaseSecretType.valueOf(engine().toUpperCase()).getJdbcPrefix();
    }

    public String getJdbcDriverClassName() {
        return DatabaseSecretType.valueOf(engine().toUpperCase()).getJdbcDriverClassName();
    }

    public String getJdbcSSLModeSuffix() {
        return DatabaseSecretType.valueOf(engine().toUpperCase()).getSslModeSuffix();
    }

    public String getDatasourceURLWithSSLMode() {
        return this.getJdbcPrefix() + host() + ":" + port() + "/" + dbname() + getJdbcSSLModeSuffix();
    }

}