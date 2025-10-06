package com.challenge.telus.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Usuario validado y enriquecido
 * Incluye campos adicionales como department_code y fecha de inserción
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidatedUser {
    
    private Long id;
    
    @JsonProperty("firstName")
    private String firstName;
    
    @JsonProperty("lastName")
    private String lastName;
    
    private String email;
    private String phone;
    private String username;
    private String birthDate;
    private String image;
    
    private Address address;
    private Company company;
    private Bank bank;
    
    // Campos adicionales para la Fase 2
    private String departmentCode;
    
    @JsonProperty("insertionDate")
    private LocalDateTime insertionDate;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        private String address;
        private String city;
        private Coordinates coordinates;
        private String postalCode;
        private String state;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Coordinates {
            private Double lat;
            private Double lng;
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Company {
        private Address address;
        private String department;
        private String name;
        private String title;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Bank {
        private String cardExpire;
        private String cardNumber;
        private String cardType;
        private String currency;
        private String iban;
    }
}



