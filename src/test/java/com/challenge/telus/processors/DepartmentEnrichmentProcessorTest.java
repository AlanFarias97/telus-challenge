package com.challenge.telus.processors;

import com.challenge.telus.models.User;
import com.challenge.telus.models.ValidatedUser;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DepartmentEnrichmentProcessor
 * Tests department code enrichment functionality
 */
class DepartmentEnrichmentProcessorTest {

    private DepartmentEnrichmentProcessor processor;
    private CamelContext camelContext;

    @BeforeEach
    void setUp() throws Exception {
        processor = new DepartmentEnrichmentProcessor();
        camelContext = new DefaultCamelContext();
        
        // Set the departments file path
        ReflectionTestUtils.setField(processor, "departmentsFile", 
            "classpath:data/departments.csv");
        
        // Initialize the processor (loads department mappings)
        processor.init();
    }

    @Test
    void testEnrichWithKnownDepartment() throws Exception {
        // Given
        User user = createUser("Engineering");
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(user);

        // When
        processor.process(exchange);

        // Then
        ValidatedUser enrichedUser = exchange.getIn().getBody(ValidatedUser.class);
        assertNotNull(enrichedUser, "Enriched user should not be null");
        assertEquals("ENG", enrichedUser.getDepartmentCode(), 
            "Engineering department should have code ENG");
    }

    @Test
    void testEnrichWithMarketingDepartment() throws Exception {
        // Given
        User user = createUser("Marketing");
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(user);

        // When
        processor.process(exchange);

        // Then
        ValidatedUser enrichedUser = exchange.getIn().getBody(ValidatedUser.class);
        assertNotNull(enrichedUser, "Enriched user should not be null");
        assertEquals("MKT", enrichedUser.getDepartmentCode(), 
            "Marketing department should have code MKT");
    }

    @Test
    void testEnrichWithUnknownDepartment() throws Exception {
        // Given
        User user = createUser("UnknownDept");
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(user);

        // When
        processor.process(exchange);

        // Then
        ValidatedUser enrichedUser = exchange.getIn().getBody(ValidatedUser.class);
        assertNotNull(enrichedUser, "Enriched user should not be null");
        assertEquals("UNKNOWN", enrichedUser.getDepartmentCode(), 
            "Unknown department should have code UNKNOWN");
    }

    @Test
    void testEnrichWithNullDepartment() throws Exception {
        // Given
        User user = createUser(null);
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(user);

        // When
        processor.process(exchange);

        // Then
        ValidatedUser enrichedUser = exchange.getIn().getBody(ValidatedUser.class);
        assertNotNull(enrichedUser, "Enriched user should not be null");
        assertEquals("UNKNOWN", enrichedUser.getDepartmentCode(), 
            "Null department should have code UNKNOWN");
    }

    @Test
    void testEnrichWithEmptyDepartment() throws Exception {
        // Given
        User user = createUser("");
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(user);

        // When
        processor.process(exchange);

        // Then
        ValidatedUser enrichedUser = exchange.getIn().getBody(ValidatedUser.class);
        assertNotNull(enrichedUser, "Enriched user should not be null");
        assertEquals("UNKNOWN", enrichedUser.getDepartmentCode(), 
            "Empty department should have code UNKNOWN");
    }

    @Test
    void testEnrichWithMultipleDepartments() throws Exception {
        // Test all known departments from departments.csv
        String[][] departments = {
            {"Marketing", "MKT"},
            {"Engineering", "ENG"},
            {"Sales", "SAL"},
            {"Human Resources", "HR"},
            {"Finance", "FIN"},
            {"Operations", "OPS"},
            {"Customer Service", "CS"},
            {"Research and Development", "RD"},
            {"Information Technology", "IT"},
            {"Legal", "LEG"},
            {"Quality Assurance", "QA"},
            {"Product Management", "PM"},
            {"Business Development", "BD"},
            {"Administration", "ADM"}
        };

        for (String[] dept : departments) {
            // Given
            User user = createUser(dept[0]);
            Exchange exchange = new DefaultExchange(camelContext);
            exchange.getIn().setBody(user);

            // When
            processor.process(exchange);

            // Then
            ValidatedUser enrichedUser = exchange.getIn().getBody(ValidatedUser.class);
            assertEquals(dept[1], enrichedUser.getDepartmentCode(), 
                String.format("%s department should have code %s", dept[0], dept[1]));
        }
    }

    @Test
    void testEnrichPreservesUserData() throws Exception {
        // Given
        User user = createUser("Engineering");
        user.setId(123);
        user.setFirstName("Jane");
        user.setLastName("Smith");
        user.setEmail("jane.smith@example.com");
        user.setAge(28);
        
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(user);

        // When
        processor.process(exchange);

        // Then
        ValidatedUser enrichedUser = exchange.getIn().getBody(ValidatedUser.class);
        assertNotNull(enrichedUser, "Enriched user should not be null");
        
        // Verify original data is preserved
        assertEquals(123, enrichedUser.getId(), "User ID should be preserved");
        assertEquals("Jane", enrichedUser.getFirstName(), "First name should be preserved");
        assertEquals("Smith", enrichedUser.getLastName(), "Last name should be preserved");
        assertEquals("jane.smith@example.com", enrichedUser.getEmail(), "Email should be preserved");
        assertEquals(28, enrichedUser.getAge(), "Age should be preserved");
        
        // Verify enrichment was added
        assertEquals("ENG", enrichedUser.getDepartmentCode(), "Department code should be added");
    }

    @Test
    void testEnrichWithCaseInsensitiveDepartment() throws Exception {
        // Given
        User user1 = createUser("engineering");  // lowercase
        User user2 = createUser("ENGINEERING");  // uppercase
        User user3 = createUser("EnGiNeErInG");  // mixed case
        
        Exchange exchange1 = new DefaultExchange(camelContext);
        Exchange exchange2 = new DefaultExchange(camelContext);
        Exchange exchange3 = new DefaultExchange(camelContext);
        
        exchange1.getIn().setBody(user1);
        exchange2.getIn().setBody(user2);
        exchange3.getIn().setBody(user3);

        // When
        processor.process(exchange1);
        processor.process(exchange2);
        processor.process(exchange3);

        // Then
        ValidatedUser enriched1 = exchange1.getIn().getBody(ValidatedUser.class);
        ValidatedUser enriched2 = exchange2.getIn().getBody(ValidatedUser.class);
        ValidatedUser enriched3 = exchange3.getIn().getBody(ValidatedUser.class);
        
        // All should map to ENG regardless of case
        assertEquals("ENG", enriched1.getDepartmentCode(), 
            "Lowercase department should map to ENG");
        assertEquals("ENG", enriched2.getDepartmentCode(), 
            "Uppercase department should map to ENG");
        assertEquals("ENG", enriched3.getDepartmentCode(), 
            "Mixed case department should map to ENG");
    }

    /**
     * Helper method to create a user with a specific department
     */
    private User createUser(String department) {
        User user = new User();
        user.setId(1);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setAge(30);
        
        User.Company company = new User.Company();
        company.setDepartment(department);
        company.setName("Test Corp");
        company.setTitle("Employee");
        user.setCompany(company);
        
        return user;
    }
}

