// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.infrastructure.exception;

import com.amazon.aws.infrastructure.helpers.SqlStateCodeType;
import com.zaxxer.hikari.SQLExceptionOverride;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;

/**
 * <p>
 * This class is used to override the SQLException that is thrown by the HikariCP connection pool.
 * </p>
 *
 * @see SQLExceptionOverride
 */
@Slf4j
public class AuroraDsqlExceptionOverride implements SQLExceptionOverride {

    @java.lang.Override
    public Override adjudicate(SQLException sqlException) {
        final String sqlState = sqlException.getSQLState();

        if (SqlStateCodeType.isRetryable(sqlState)) {
            log.info("Encountered retryable SQL exception: {}", sqlException.getMessage());
            return SQLExceptionOverride.Override.DO_NOT_EVICT;
        }

        return Override.CONTINUE_EVICT;
    }
}