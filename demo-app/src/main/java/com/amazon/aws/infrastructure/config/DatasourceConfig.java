// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.infrastructure.config;

import com.amazon.aws.infrastructure.dto.DatabaseSecret;
import com.amazon.aws.infrastructure.exception.AuroraDsqlExceptionOverride;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * <p>Configuration class for the datasource bean</p>
 * <p>Uses the AWSSecretsManagerConfig to get the secret from AWS Secrets Manager</p>
 */
@Configuration
@Profile("!local")
public class DatasourceConfig {

    private final AWSSecretsManagerConfig AWSSecretsManagerConfig;

    @Autowired
    public DatasourceConfig(AWSSecretsManagerConfig AWSSecretsManagerConfig) {
        this.AWSSecretsManagerConfig = AWSSecretsManagerConfig;
    }

    @Bean
    public DataSource getDataSource() {
        DatabaseSecret secret = AWSSecretsManagerConfig.getSecret();

        HikariConfig hikariConfig = new HikariConfig();

        // Set basic connection properties
        hikariConfig.setDriverClassName(secret.getJdbcDriverClassName());
        hikariConfig.setJdbcUrl(secret.getDatasourceURLWithSSLMode());
        hikariConfig.setUsername(secret.username());
        hikariConfig.setPassword(secret.password());

        // Set custom exception override
        hikariConfig.setExceptionOverrideClassName(AuroraDsqlExceptionOverride.class.getName());

        return new HikariDataSource(hikariConfig);
    }
}