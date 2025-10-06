package com.challenge.telus.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Modelo de datos para representar un usuario de la API
 * Basado en la estructura de respuesta de https://dummyjson.com/users
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    
    private Long id;
    
    @JsonProperty("firstName")
    private String firstName;
    
    @JsonProperty("lastName")
    private String lastName;
    
    private String email;
    
    private Integer age;
    
    private String phone;
    
    private String username;
    
    private String password;
    
    private String birthDate;
    
    private String image;
    
    private Address address;
    
    private Company company;
    
    private Bank bank;
    
    private Crypto crypto;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Address {
        private String address;
        private String city;
        private Coordinates coordinates;
        private String postalCode;
        private String state;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Coordinates {
            private Double lat;
            private Double lng;
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Company {
        private Address address;
        private String department;
        private String name;
        private String title;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bank {
        private String cardExpire;
        private String cardNumber;
        private String cardType;
        private String currency;
        private String iban;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Crypto {
        private String coin;
        private String wallet;
        private String network;
    }
}
