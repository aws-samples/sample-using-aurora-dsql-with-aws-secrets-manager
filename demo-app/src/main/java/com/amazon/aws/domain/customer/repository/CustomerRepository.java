// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.domain.customer.repository;

import com.amazon.aws.domain.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing Customer entities.
 * <p>
 * This interface extends JpaRepository and is responsible for handling CRUD operations
 * for the Customer entity. It uses UUID as the type for the entity's primary key.
 * </p>
 *
 * @see Customer
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    /**
     * Simulates a long-running query using pg_sleep for testing transaction timeout scenarios.
     * WARNING: This method is for testing purposes only.
     *
     * @param seconds The number of seconds to sleep
     */
    @Query(value = "SELECT PG_SLEEP(:seconds)", nativeQuery = true)
    void timeoutSimulation(@Param("seconds") int seconds);

    /**
     * Finds the first Customer entity with a non-null ID.
     *
     * @return An Optional containing the first Customer entity with a non-null ID, or an empty Optional if none found.
     */
    Optional<Customer> findFirstByIdIsNotNull();

}