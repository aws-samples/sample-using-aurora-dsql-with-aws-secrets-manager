// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.domain.customer.service;

import com.amazon.aws.domain.customer.entity.Customer;
import com.amazon.aws.domain.customer.repository.CustomerRepository;
import com.amazon.aws.infrastructure.config.AppConfig;
import com.amazon.aws.infrastructure.helpers.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final RetryTemplate retryTemplate;
    private final AppConfig appConfig;

    public CustomerService(final CustomerRepository customerRepository, RetryTemplate retryTemplate, AppConfig appConfig) {
        this.customerRepository = customerRepository;
        this.retryTemplate = retryTemplate;
        this.appConfig = appConfig;
    }

    /**
     * <p>
     * Creates a new customer.
     * </p>
     *
     * @param customer The customer data to create.
     * @return The created customer data
     */
    public Customer createCustomer(final Customer customer) {
        return this.retryTemplate.execute(context -> {
            log.info("Attempting to create/update customer. Attempt #{}", context.getRetryCount() + 1);
            log.info("Creating new customer and addresses");
            customer.validateAddresses();
            customer.createAddresses();
            return this.customerRepository.save(customer);
        }, context -> {
            log.error("Failed to create customer after {} attempts", context.getRetryCount());
            throw Util.handleRetryFailure(context);
        });
    }

    /**
     * <p>
     * Updates the customer data.
     * </p>
     *
     * @param existingCustomer The existing customer data.
     * @param customer         The customer data to update.
     * @return The updated customer data
     */
    public Customer updateCustomer(final Customer existingCustomer, final Customer customer) {
        return this.retryTemplate.execute(context -> {
            log.info("Attempting to update customer. Attempt #{}", context.getRetryCount() + 1);
            log.info("Updating existing customer [id={}]", customer.getId());
            updateCustomerInformation(existingCustomer, customer);
            return this.customerRepository.save(existingCustomer);
        }, context -> {
            log.error("Failed to update customer after {} attempts", context.getRetryCount());
            throw Util.handleRetryFailure(context);
        });
    }

    /**
     * <p>
     * Updates the customer fields with the provided data.
     * </p>
     *
     * @param existingCustomer The existing customer data.
     * @param customer         The customer data to update.
     */
    private void updateCustomerInformation(Customer existingCustomer, Customer customer) {
        existingCustomer.setFirstName(customer.getFirstName());
        existingCustomer.setLastName(customer.getLastName());
        existingCustomer.setEmail(customer.getEmail());

        // Validate new addresses
        customer.validateAddresses();

        // Update existing addresses using the helper method
        existingCustomer.updateAddresses(customer.getAddresses());
    }

    /**
     * Triggers a simulation of optimistic locking scenarios.
     * <p>
     * WARNING: This method is for testing purposes only.
     * </p>
     * <p>
     * This method uses the CustomerRepository to simulate concurrent updates
     * and optimistic locking scenarios.
     * </p>
     * <p>
     * This method is not intended to be used in production.
     * </p>
     */
    public void triggerOptimisticLockingSimulation(Customer customer) {
        this.retryTemplate.execute(context -> {
            log.info("Attempting to execute locking simulation. Attempt #{}", context.getRetryCount() + 1);
            this.customerRepository.save(customer);
            return null;  // Required for void operations
        }, context -> {
            log.error("Failed to execute locking simulation after {} attempts", context.getRetryCount());
            throw Util.handleRetryFailure(context);
        });
    }

    /**
     * Triggers a simulation of transaction timeout scenarios.
     * <p>
     * WARNING: This method is for testing purposes only.
     * </p>
     * <p>
     * This method uses the CustomerRepository to simulate a long-running query
     * using pg_sleep for testing transaction timeout scenarios.
     * </p>
     * <p>
     * The method will take twice the configured timeout duration to ensure
     * that the transaction times out.
     * </p>
     * <p>
     * This method is not intended to be used in production.
     * </p>
     */
    public void triggerTimeoutSimulation() {
        this.retryTemplate.execute(context -> {
            log.info("Attempting to execute timeout simulation. Attempt #{}", context.getRetryCount() + 1);
            // Enforcing twice the maximum transaction timeout
            int timeoutDuration = this.appConfig.getTransactionTimeoutInSeconds() * 2;
            this.customerRepository.timeoutSimulation(timeoutDuration);
            return null;  // Required for void operations
        }, context -> {
            log.error("Failed to execute timeout simulation after {} attempts", context.getRetryCount());
            throw Util.handleRetryFailure(context);
        });
    }
}
