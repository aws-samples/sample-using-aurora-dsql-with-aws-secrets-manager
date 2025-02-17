// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws;

import com.amazon.aws.domain.customer.controller.CustomerController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DemoAppApplicationTests {

    @Autowired
    private CustomerController customerController;

    @Test
    void contextLoads() {
        assertThat(customerController).isNotNull();
    }

}
