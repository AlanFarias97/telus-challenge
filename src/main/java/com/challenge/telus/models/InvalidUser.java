package com.challenge.telus.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Usuario inválido con información de error
 * Se guarda en el Dead Letter Queue
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvalidUser {
    
    private User originalUser;
    private List<String> errorReasons;
    
    @JsonProperty("errorTimestamp")
    private LocalDateTime errorTimestamp;
    
    /**
     * Crea un usuario inválido desde un ValidationResult
     */
    public static InvalidUser fromValidationResult(ValidationResult result) {
        InvalidUser invalidUser = new InvalidUser();
        invalidUser.setOriginalUser(result.getUser());
        invalidUser.setErrorReasons(result.getErrors());
        invalidUser.setErrorTimestamp(LocalDateTime.now());
        return invalidUser;
    }
    
    /**
     * Crea un usuario inválido con un solo error
     */
    public static InvalidUser withError(User user, String errorReason) {
        InvalidUser invalidUser = new InvalidUser();
        invalidUser.setOriginalUser(user);
        invalidUser.setErrorReasons(List.of(errorReason));
        invalidUser.setErrorTimestamp(LocalDateTime.now());
        return invalidUser;
    }
}



