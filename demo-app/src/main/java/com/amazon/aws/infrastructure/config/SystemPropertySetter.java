// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <p>System property setter</p>
 * <p>Set the DNS cache TTL to 60 seconds</p>
 */
@Slf4j
@Component
public class SystemPropertySetter {

    @PostConstruct
    public void setProperty() {
        log.info("Setting the DNS cache TTL to 60 seconds");
        java.security.Security.setProperty("networkaddress.cache.ttl", "60");
    }

}