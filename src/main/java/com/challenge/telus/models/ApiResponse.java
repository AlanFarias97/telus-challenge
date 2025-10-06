package com.challenge.telus.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Modelo para la respuesta paginada de la API de usuarios
 * Estructura: { "users": [...], "total": 100, "skip": 0, "limit": 10 }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    
    @JsonProperty("users")
    private List<User> users;
    
    private Integer total;
    
    private Integer skip;
    
    private Integer limit;
    
    /**
     * Verifica si hay más páginas por procesar
     * @return true si skip + limit < total
     */
    public boolean hasMorePages() {
        return (skip + limit) < total;
    }
    
    /**
     * Obtiene el siguiente valor de skip
     * @return skip + limit
     */
    public Integer getNextSkip() {
        return skip + limit;
    }
    
    /**
     * Verifica si la respuesta está vacía
     * @return true si no hay usuarios o la lista está vacía
     */
    public boolean isEmpty() {
        return users == null || users.isEmpty();
    }
}

