// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.domain.customer.entity;

import com.amazon.aws.infrastructure.exception.AddressValidationException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * Represents a customer entity in the system.
 * <p>
 * This class is mapped to the "customers" table in the database and serves as the
 * core entity for customer information. It includes personal details and associated
 * addresses.
 * </p>
 *
 * @Entity Marks this class as a JPA entity.
 * @Table(name = "customers") Specifies the database table name for this entity.
 * @Getter Lombok annotation to automatically generate getter methods for all fields.
 * @Setter Lombok annotation to automatically generate setter methods for all fields.
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @NotBlank(message = "First name is required")
    private String firstName;

    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @NotNull(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @JsonIgnore
    private boolean isActive = true;

    @Valid
    @NotEmpty(message = "At least one address is required")
    @Size(max = 5, message = "Customer cannot have more than 5 addresses")
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();

    public Customer() {
    }

    public Customer(String firstName, String lastName, String email, boolean isActive, List<Address> addresses) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.isActive = isActive;
        this.addresses = addresses;
    }

    // Helper method to manage bidirectional relationship
    public void addAddress(Address address) {
        this.addresses.add(address);
        address.setCustomer(this);
    }

    // Helper method to manage bidirectional relationship
    public void removeAddress(Address address) {
        if (this.addresses.remove(address)) {
            address.setCustomer(null);

            // If removed address was default and there are other addresses,
            // make the first one default
            if (address.isDefault() && !this.addresses.isEmpty()) {
                this.addresses.get(0).setDefault(true);
            }
        }
    }

    // Helper method to update addresses
    public void updateAddresses(List<Address> newAddresses) {
        // Create a copy of the existing addresses to avoid concurrent modification
        List<Address> existingAddresses = new ArrayList<>(this.addresses);

        // Remove all existing addresses
        existingAddresses.forEach(this::removeAddress);

        // Add new addresses
        if (newAddresses != null) {
            newAddresses.forEach(this::addAddress);
        }
    }

    // Helper method to create addresses
    public void createAddresses() {
        if (this.addresses != null) {
            this.addresses.forEach(address -> {
                address.setCustomer(this);
            });
        }
    }

    /**
     * <p>Validates the addresses list.</p>
     * <p>Ensures that the list is not null, not empty, has no more than 5 addresses, and has only one default address.</p>
     * <p>Also, it ensures that each address is not null and makes the list unmodifiable.</p>
     * <p>Throws an AddressValidationException if any validation fails.</p>
     *
     * @throws AddressValidationException if the addresses list is invalid
     */
    public void validateAddresses() {
        if (this.addresses == null) {
            throw new AddressValidationException("Addresses list cannot be null");
        }

        if (this.addresses.isEmpty()) {
            throw new AddressValidationException("Customer must have at least one address");
        }

        if (this.addresses.size() > 5) {
            throw new AddressValidationException("Customer cannot have more than 5 addresses");
        }

        // Validate only one default address
        long defaultAddressCount = this.addresses.stream()
                .filter(Address::isDefault)
                .count();

        if (defaultAddressCount > 1) {
            throw new AddressValidationException("Only one address can be set as default");
        }

        // Validate each address is not null
        if (this.addresses.stream().anyMatch(Objects::isNull)) {
            throw new AddressValidationException("Null addresses are not allowed");
        }

        // If no default address is set, set the first one as default
        if (defaultAddressCount == 0) {
            this.addresses.get(0).setDefault(true);
        }

        // Make the list unmodifiable
        this.addresses = List.copyOf(this.addresses);
    }

    @PrePersist
    @PreUpdate
    public void sanitizeData() {
        // Sanitize firstName
        firstName = Optional.ofNullable(firstName)
                .map(String::trim)
                .orElse(null);

        // Sanitize lastName
        lastName = Optional.ofNullable(lastName)
                .map(String::trim)
                .orElse(null);

        // Sanitize email
        email = Optional.ofNullable(email)
                .map(String::trim)
                .map(String::toLowerCase)
                .orElse(null);
    }
}

