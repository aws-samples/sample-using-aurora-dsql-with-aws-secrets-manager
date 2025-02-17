// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.domain.customer.controller;

import com.amazon.aws.domain.customer.entity.Address;
import com.amazon.aws.domain.customer.entity.Customer;
import com.amazon.aws.domain.customer.repository.CustomerRepository;
import com.amazon.aws.domain.customer.service.CustomerService;
import com.amazon.aws.infrastructure.exception.AddressValidationException;
import com.amazon.aws.infrastructure.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    private static Validator validator;
    private final UUID TEST_UUID = UUID.randomUUID();
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private CustomerService customerService;
    @MockBean
    private CustomerRepository customerRepository;
    private Customer validCustomerRequest;
    private Customer mockCustomer;

    @BeforeAll
    static void setupValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @BeforeEach
    void setUp() {
        // Setup valid address request
        Address addressRequest = new Address("123 Main St", "Seattle", "WA", "98101", "USA", true);

        // Setup valid customer request
        validCustomerRequest = new Customer("John", "Doe", "john.doe@example.com", true, List.of(addressRequest));

        // Setup mock customer
        mockCustomer = new Customer();
        mockCustomer.setId(TEST_UUID);
        mockCustomer.setFirstName("John");
        mockCustomer.setLastName("Doe");
        mockCustomer.setEmail("john.doe@example.com");
    }

    @Nested
    @DisplayName("Create Customer Tests")
    class CreateCustomerTests {
        @Test
        @DisplayName("Should create customer successfully with valid request")
        void createCustomer_WithValidRequest_ShouldReturnCreated() throws Exception {
            when(customerService.createCustomer(any(Customer.class))).thenReturn(mockCustomer);

            MvcResult result = mockMvc.perform(post("/api/v1/customers").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(validCustomerRequest))).andExpect(status().isCreated()).andReturn();
            Customer responseCustomer = objectMapper.readValue(result.getResponse().getContentAsString(), Customer.class);

            assertThat(responseCustomer).isNotNull().satisfies(customer -> {
                assertThat(customer.getId()).isEqualTo(TEST_UUID);
                assertThat(customer.getFirstName()).isEqualTo("John");
                assertThat(customer.getLastName()).isEqualTo("Doe");
                assertThat(customer.getEmail()).isEqualTo("john.doe@example.com");
            });

            verify(customerService, times(1)).createCustomer(any(Customer.class));
        }

        @Test
        @DisplayName("Should return BadRequest when creating customer with invalid email")
        void createCustomer_WithInvalidEmail_ShouldReturnBadRequest() throws Exception {
            Customer invalidRequest = new Customer("John", "Doe", "invalid-email", true, Collections.emptyList());

            mockMvc.perform(post("/api/v1/customers").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invalidRequest))).andExpect(status().isBadRequest());

            verify(customerService, never()).createCustomer(any());
        }
    }

    @Nested
    @DisplayName("Get Customer Tests")
    class GetCustomerTests {
        @Test
        @DisplayName("Should return customer when valid ID is provided")
        void getCustomer_WithValidId_ShouldReturnCustomer() throws Exception {
            when(customerRepository.findById(TEST_UUID)).thenReturn(Optional.ofNullable(mockCustomer));

            MvcResult result = mockMvc.perform(get("/api/v1/customers/{id}", TEST_UUID)).andExpect(status().isOk()).andReturn();
            Customer responseCustomer = objectMapper.readValue(result.getResponse().getContentAsString(), Customer.class);

            assertThat(responseCustomer).isNotNull().satisfies(customer -> {
                assertThat(customer.getId()).isEqualTo(TEST_UUID);
                assertThat(customer.getFirstName()).isEqualTo("John");
            });

            verify(customerRepository, times(1)).findById(TEST_UUID);
        }

        @Test
        @DisplayName("Should return NotFound when customer doesn't exist")
        void getCustomer_WithInvalidId_ShouldReturnNotFound() throws Exception {
            when(customerRepository.findById(any(UUID.class))).thenThrow(new ResourceNotFoundException("Customer not found"));
            mockMvc.perform(get("/api/v1/customers/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Update Customer Tests")
    class UpdateCustomerTests {
        @Test
        @DisplayName("Should update customer successfully with valid request")
        void updateCustomer_WithValidRequest_ShouldReturnUpdatedCustomer() throws Exception {
            when(customerService.updateCustomer(any(Customer.class), any(Customer.class))).thenReturn(mockCustomer);
            when(customerRepository.findById(TEST_UUID)).thenReturn(Optional.ofNullable(mockCustomer));

            mockMvc.perform(put("/api/v1/customers/{id}", TEST_UUID).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(validCustomerRequest))).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(TEST_UUID.toString()));
            verify(customerService, times(1)).updateCustomer(any(Customer.class), any(Customer.class));
        }
    }

    @Nested
    @DisplayName("Delete Customer Tests")
    class DeleteCustomerTests {
        @Test
        void deleteCustomer_WithValidId_ShouldReturnNoContent() throws Exception {
            when(customerRepository.findById(TEST_UUID)).thenReturn(Optional.ofNullable(mockCustomer));
            mockMvc.perform(delete("/api/v1/customers/{id}", TEST_UUID)).andExpect(status().isNoContent());
            verify(customerRepository, times(1)).findById(TEST_UUID);
        }

    }

    @Nested
    @DisplayName("Get All Customers Tests")
    class GetAllCustomersTests {
        @Test
        @DisplayName("Should return list of all customers")
        void getAllCustomers_ShouldReturnCustomersList() throws Exception {
            List<Customer> customers = Collections.singletonList(mockCustomer);
            Pageable pageable = PageRequest.of(0, 20);
            when(customerRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(customers, pageable, customers.size()));

            MvcResult result = mockMvc.perform(get("/api/v1/customers")).andExpect(status().isOk()).andReturn();
            List<Customer> responseCustomers = objectMapper.readValue(result.getResponse().getContentAsString(), objectMapper.getTypeFactory().constructCollectionType(List.class, Customer.class));

            assertThat(responseCustomers).isNotEmpty().hasSize(1).first().satisfies(customer -> {
                assertThat(customer.getId()).isEqualTo(TEST_UUID);
                assertThat(customer.getFirstName()).isEqualTo("John");
                assertThat(customer.getLastName()).isEqualTo("Doe");
            });

            verify(customerRepository, times(1)).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty list when no customers exist")
        void getAllCustomers_WhenNoCustomers_ShouldReturnEmptyList() throws Exception {
            // Do this instead - explicitly return an empty page if needed
            when(customerRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList()));
            MvcResult result = mockMvc.perform(get("/api/v1/customers")).andExpect(status().isOk()).andReturn();
            List<Customer> responseCustomers = objectMapper.readValue(result.getResponse().getContentAsString(), objectMapper.getTypeFactory().constructCollectionType(List.class, Customer.class));
            assertThat(responseCustomers).isEmpty();
        }
    }

    @Nested
    class CustomerRequestValidationTests {
        @Test
        void shouldThrowException_WhenMultipleDefaultAddresses() {
            List<Address> addresses = List.of(new Address("123 Main St", "Seattle", "WA", "98101", "USA", true), new Address("456 Second St", "Seattle", "WA", "98101", "USA", true));
            Customer customer = new Customer("John", "Doe", "john@example.com", true, addresses);
            assertThrows(AddressValidationException.class, customer::validateAddresses);
        }

        @Test
        void shouldValidate_EmailFormat() {
            Customer invalidRequest = new Customer("John", "Doe", "invalid-email", true, null);
            Set<ConstraintViolation<Customer>> violations = validator.validate(invalidRequest);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("email")));
        }

    }
}
