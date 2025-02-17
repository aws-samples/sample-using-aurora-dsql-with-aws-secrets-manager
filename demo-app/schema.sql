-- Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
-- SPDX-License-Identifier: MIT-0

-- Drop indexes
DROP INDEX IF EXISTS first_name_idx;
DROP INDEX IF EXISTS last_name_idx;
DROP INDEX IF EXISTS city_idx;

-- Drop tables
DROP TABLE IF EXISTS addresses;
DROP TABLE IF EXISTS customers;

-- Customers table
CREATE TABLE customers
(
    id         UUID PRIMARY KEY,
    first_name VARCHAR(255),
    last_name  VARCHAR(255),
    email      VARCHAR(255) UNIQUE,
    is_active  BOOLEAN DEFAULT TRUE
);

-- Index on first name column
CREATE INDEX first_name_idx on customers (first_name);

-- Index on last name column
CREATE INDEX last_name_idx on customers (last_name);

-- Addresses table
-- Foreign key is not supported today by Amazon Aurora DSQL
CREATE TABLE addresses
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    street      VARCHAR(255),
    city        VARCHAR(255),
    state       VARCHAR(255),
    postal_code VARCHAR(255),
    country     VARCHAR(255),
    is_default  BOOLEAN DEFAULT FALSE,
    customer_id UUID
);

-- Index on city column
CREATE INDEX city_idx on addresses (city);