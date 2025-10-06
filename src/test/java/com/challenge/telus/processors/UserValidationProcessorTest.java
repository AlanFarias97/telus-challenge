package com.challenge.telus.processors;

import com.challenge.telus.models.User;
import com.challenge.telus.models.ValidationResult;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserValidationProcessor
 * Tests business rule validation for user data
 */
class UserValidationProcessorTest {

    private UserValidationProcessor processor;
    private CamelContext camelContext;

    @BeforeEach
    void setUp() {
        processor = new UserValidationProcessor();
        camelContext = new DefaultCamelContext();
        
        // Set the schema file path
        ReflectionTestUtils.setField(processor, "schemaFile", 
            "classpath:schemas/user-validation-schema.json");
    }

    @Test
    void testValidUser() throws Exception {
        // Given
        User validUser = createValidUser();
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(validUser);

        // When
        processor.process(exchange);

        // Then
        ValidationResult result = exchange.getProperty("validationResult", ValidationResult.class);
        assertNotNull(result, "Validation result should not be null");
        assertTrue(result.isValid(), "Valid user should pass validation");
        assertTrue(result.getErrors().isEmpty(), "Valid user should have no errors");
    }

    @Test
    void testUserWithInvalidEmail() throws Exception {
        // Given
        User user = createValidUser();
        user.setEmail("invalid-email-format"); // Invalid email
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(user);

        // When
        processor.process(exchange);

        // Then
        ValidationResult result = exchange.getProperty("validationResult", ValidationResult.class);
        assertNotNull(result, "Validation result should not be null");
        assertFalse(result.isValid(), "User with invalid email should fail validation");
        assertTrue(result.getErrors().stream()
                .anyMatch(error -> error.toLowerCase().contains("email")),
                "Errors should mention email");
    }

    @Test
    void testUserWithInvalidAge() throws Exception {
        // Given
        User youngUser = createValidUser();
        youngUser.setAge(15); // Too young (< 18)
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(youngUser);

        // When
        processor.process(exchange);

        // Then
        ValidationResult result = exchange.getProperty("validationResult", ValidationResult.class);
        assertNotNull(result, "Validation result should not be null");
        assertFalse(result.isValid(), "User under 18 should fail validation");
        assertTrue(result.getErrors().stream()
                .anyMatch(error -> error.toLowerCase().contains("age")),
                "Errors should mention age");
    }

    @Test
    void testUserWithAgeTooOld() throws Exception {
        // Given
        User oldUser = createValidUser();
        oldUser.setAge(150); // Too old (> 120)
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(oldUser);

        // When
        processor.process(exchange);

        // Then
        ValidationResult result = exchange.getProperty("validationResult", ValidationResult.class);
        assertNotNull(result, "Validation result should not be null");
        assertFalse(result.isValid(), "User over 120 should fail validation");
    }

    @Test
    void testUserWithMissingRequiredFields() throws Exception {
        // Given
        User user = new User();
        user.setId(1);
        // Missing firstName, lastName, email, etc.
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(user);

        // When
        processor.process(exchange);

        // Then
        ValidationResult result = exchange.getProperty("validationResult", ValidationResult.class);
        assertNotNull(result, "Validation result should not be null");
        assertFalse(result.isValid(), "User with missing fields should fail validation");
        assertFalse(result.getErrors().isEmpty(), "Should have multiple errors for missing fields");
    }

    @Test
    void testUserWithEmptyEmail() throws Exception {
        // Given
        User user = createValidUser();
        user.setEmail(""); // Empty email
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(user);

        // When
        processor.process(exchange);

        // Then
        ValidationResult result = exchange.getProperty("validationResult", ValidationResult.class);
        assertNotNull(result, "Validation result should not be null");
        assertFalse(result.isValid(), "User with empty email should fail validation");
    }

    @Test
    void testUserWithNullEmail() throws Exception {
        // Given
        User user = createValidUser();
        user.setEmail(null); // Null email
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(user);

        // When
        processor.process(exchange);

        // Then
        ValidationResult result = exchange.getProperty("validationResult", ValidationResult.class);
        assertNotNull(result, "Validation result should not be null");
        assertFalse(result.isValid(), "User with null email should fail validation");
    }

    @Test
    void testUserAtMinimumAge() throws Exception {
        // Given
        User user = createValidUser();
        user.setAge(18); // Minimum valid age
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(user);

        // When
        processor.process(exchange);

        // Then
        ValidationResult result = exchange.getProperty("validationResult", ValidationResult.class);
        assertNotNull(result, "Validation result should not be null");
        assertTrue(result.isValid(), "User at minimum age (18) should pass validation");
    }

    @Test
    void testUserAtMaximumAge() throws Exception {
        // Given
        User user = createValidUser();
        user.setAge(120); // Maximum valid age
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(user);

        // When
        processor.process(exchange);

        // Then
        ValidationResult result = exchange.getProperty("validationResult", ValidationResult.class);
        assertNotNull(result, "Validation result should not be null");
        assertTrue(result.isValid(), "User at maximum age (120) should pass validation");
    }

    /**
     * Helper method to create a valid user for testing
     */
    private User createValidUser() {
        User user = new User();
        user.setId(1);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setAge(30);
        user.setPhone("+1-555-0123");
        user.setUsername("johndoe");
        
        // Address
        User.Address address = new User.Address();
        address.setAddress("123 Main St");
        address.setCity("Springfield");
        address.setState("IL");
        address.setPostalCode("62701");
        address.setCountry("United States");
        user.setAddress(address);
        
        // Company
        User.Company company = new User.Company();
        company.setName("Tech Corp");
        company.setTitle("Software Engineer");
        company.setDepartment("Engineering");
        user.setCompany(company);
        
        return user;
    }
}

