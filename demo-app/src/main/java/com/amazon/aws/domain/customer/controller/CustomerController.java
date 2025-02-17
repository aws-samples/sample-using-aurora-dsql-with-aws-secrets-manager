// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.domain.customer.controller;

import com.amazon.aws.domain.customer.entity.Customer;
import com.amazon.aws.domain.customer.repository.CustomerRepository;
import com.amazon.aws.domain.customer.service.CustomerService;
import com.amazon.aws.infrastructure.helpers.Util;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Controller class for handling customer-related operations.
 * <p>
 * This class is responsible for managing customer-related requests and responses.
 * It interacts with the CustomerService and CustomerRepository to perform CRUD operations
 * on customer data.
 * </p>
 *
 * @see CustomerService
 * @see CustomerRepository
 */
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final CustomerService customerService;

    public CustomerController(CustomerRepository customerRepository, CustomerService customerService) {
        this.customerRepository = customerRepository;
        this.customerService = customerService;
    }

    /**
     * Retrieves a paginated list of all customers.
     *
     * @param page The page number to retrieve (zero-based).
     * @param size The number of customers to retrieve per page.
     * @return A ResponseEntity containing the paginated list of customers.
     */
    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Customer> customerPage = customerRepository.findAll(pageable);
        return ResponseEntity.ok(customerPage.getContent());
    }

    /**
     * Retrieves a customer by their unique identifier.
     *
     * @param id The unique identifier of the customer.
     * @return A ResponseEntity containing the customer data if found, or a 404 Not Found response if not found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable UUID id) {
        return customerRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new customer based on the provided request data.
     *
     * @param customer The customer data to create.
     * @return A ResponseEntity containing the customer data if created
     */
    @PostMapping
    public ResponseEntity<Customer> createCustomer(@Valid @RequestBody Customer customer) {
        Customer response = customerService.createCustomer(customer);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Updates an existing customer based on the provided request data.
     *
     * @param id The unique identifier of the customer to update.
     * @return A ResponseEntity containing the updated customer data if found, or a 404 Not Found response if not found.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(@PathVariable UUID id, @Valid @RequestBody Customer customer) {
        return customerRepository.findById(id).map(existingCustomer -> ResponseEntity.ok(customerService.updateCustomer(existingCustomer, customer))).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Simulate optimistic locking exception handling for the update.
     */
    @GetMapping("/simulate/locking")
    public void simulateOptimisticLocking() {
        Customer customer = customerRepository.findFirstByIdIsNotNull().orElseThrow(() -> new EntityNotFoundException("No customer found"));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Runnable concurrentTask = getRunnableTask(customer);
            CompletableFuture<Void> task1 = CompletableFuture.runAsync(concurrentTask, executor);
            CompletableFuture<Void> task2 = CompletableFuture.runAsync(concurrentTask, executor);

            CompletableFuture.allOf(task1, task2).join();
        } catch (CompletionException e) {
            if (e.getCause() != null) {
                throw (RuntimeException) e.getCause();
            }
            throw e;
        } finally {
            executor.shutdown();
            Thread.currentThread().interrupt();
        }

    }

    /**
     * Get a Runnable task for concurrent execution.
     *
     * @param customer The customer object to be updated.
     * @return A Runnable task for concurrent execution.
     */
    private Runnable getRunnableTask(Customer customer) {
        CyclicBarrier barrier = new CyclicBarrier(2);

        // Wait for other thread
        return () -> {
            try {
                customer.setFirstName(Util.generateRandomName());
                customer.setLastName(Util.generateRandomName());
                barrier.await(); // Wait for other thread
                customerService.triggerOptimisticLockingSimulation(customer);
            } catch (InterruptedException | BrokenBarrierException e) {
                Thread.currentThread().interrupt();
                throw new CompletionException(e);
            }
        };
    }

    /**
     * Simulate timeout exception handling for the update.
     **/
    @GetMapping("/simulate/timeout")
    public void simulateTimeout() {
        customerService.triggerTimeoutSimulation();
    }

    /**
     * Deletes a customer by their unique identifier.
     *
     * @param id The unique identifier of the customer to delete.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCustomer(@PathVariable UUID id) {
        customerRepository.findById(id).ifPresentOrElse(customerRepository::delete, () -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found");
        });
    }

}
