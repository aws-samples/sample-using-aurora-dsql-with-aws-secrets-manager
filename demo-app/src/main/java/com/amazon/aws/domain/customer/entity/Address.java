// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.domain.customer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;
import java.util.UUID;

/**
 * Represents an address entity in the system.
 * <p>
 * This class is mapped to the "addresses" table in the database and stores
 * detailed address information for customers. Each address is associated with
 * a customer and can be marked as the default address.
 *
 * @Entity Marks this class as a JPA entity.
 * @Table(name = "addresses") Specifies the database table name for this entity.
 * @Getter Lombok annotation to automatically generate getter methods for all fields.
 * @Setter Lombok annotation to automatically generate setter methods for all fields.
 */
@Entity
@Table(name = "addresses")
@Getter
@Setter
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid DEFAULT gen_random_uuid()")
    private UUID id;

    @NotBlank(message = "Street is required")
    private String street;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Postal code is required")
    private String postalCode;

    @NotBlank(message = "Country is required")
    private String country;

    private boolean isDefault;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    public Address() {
    }

    public Address(String street, String city, String state, String postalCode, String country, boolean isDefault) {
        this.street = street;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
        this.isDefault = isDefault;
    }

    @PrePersist
    @PreUpdate
    protected void sanitizeData() {
        // Sanitize street
        street = Optional.ofNullable(street)
                .map(String::trim)
                .orElse(null);

        // Sanitize city
        city = Optional.ofNullable(city)
                .map(String::trim)
                .orElse(null);

        // Sanitize state
        state = Optional.ofNullable(state)
                .map(String::trim)
                .orElse(null);

        // Sanitize postal code
        postalCode = Optional.ofNullable(postalCode)
                .map(String::trim)
                .map(code -> code.replaceAll("\\s+", ""))  // Remove all whitespace
                .map(String::toUpperCase)
                .orElse(null);

        // Sanitize country
        country = Optional.ofNullable(country)
                .map(String::trim)
                .orElse(null);
    }
}

