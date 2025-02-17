// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.domain.customer.repository;

import com.amazon.aws.domain.customer.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for managing Address entities.
 * <p>
 * This interface extends JpaRepository and is responsible for handling CRUD operations
 * for the Address entity. It uses UUID as the type for the entity's primary key.
 * </p>
 *
 * @see Address
 */
@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {
}
