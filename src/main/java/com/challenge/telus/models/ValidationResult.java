package com.challenge.telus.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Resultado de la validación de un usuario
 * Contiene el usuario y los errores de validación si los hay
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    
    private User user;
    private boolean valid;
    private List<String> errors;
    
    /**
     * Crea un resultado válido
     */
    public static ValidationResult valid(User user) {
        return new ValidationResult(user, true, null);
    }
    
    /**
     * Crea un resultado inválido con errores
     */
    public static ValidationResult invalid(User user, List<String> errors) {
        return new ValidationResult(user, false, errors);
    }
    
    /**
     * Verifica si el resultado es válido
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Verifica si el resultado es inválido
     */
    public boolean isInvalid() {
        return !valid;
    }
}



