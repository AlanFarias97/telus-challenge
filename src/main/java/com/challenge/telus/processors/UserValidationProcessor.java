package com.challenge.telus.processors;

import com.challenge.telus.models.User;
import com.challenge.telus.models.ValidationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Procesador para validar usuarios contra el esquema JSON
 * Valida campos requeridos y formatos
 */
@Slf4j
@Component
public class UserValidationProcessor implements Processor {

    private final ObjectMapper objectMapper;
    private final Resource schemaResource;

    public UserValidationProcessor(
            @Value("classpath:schemas/user-validation-schema.json") Resource schemaResource) {
        this.objectMapper = new ObjectMapper();
        this.schemaResource = schemaResource;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        User user = exchange.getIn().getBody(User.class);

        if (user == null) {
            log.warn("Usuario es null, saltando validación");
            exchange.getMessage().setBody(ValidationResult.invalid(user, List.of("Usuario es null")));
            return;
        }

        log.debug("Validando usuario: {}", user.getId());

        List<String> errors = validateUser(user);

        if (errors.isEmpty()) {
            log.debug("Usuario {} válido", user.getId());
            exchange.getMessage().setBody(ValidationResult.valid(user));
        } else {
            log.warn("Usuario {} inválido: {}", user.getId(), errors);
            exchange.getMessage().setBody(ValidationResult.invalid(user, errors));
        }
    }

    /**
     * Valida un usuario contra las reglas de negocio
     */
    private List<String> validateUser(User user) {
        List<String> errors = new ArrayList<>();

        // Validar ID
        if (user.getId() == null || user.getId() <= 0) {
            errors.add("ID debe ser un entero positivo");
        }

        // Validar firstName
        if (user.getFirstName() == null || user.getFirstName().trim().isEmpty()) {
            errors.add("firstName es requerido y no puede estar vacío");
        }

        // Validar email
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            errors.add("email es requerido");
        } else if (!isValidEmail(user.getEmail())) {
            errors.add("email debe tener formato válido");
        }

        // Validar age
        if (user.getAge() == null) {
            errors.add("age es requerido");
        } else if (user.getAge() < 18 || user.getAge() > 65) {
            errors.add("age debe estar entre 18 y 65 años");
        }

        // Validar company.department
        if (user.getCompany() == null) {
            errors.add("company es requerido");
        } else if (user.getCompany().getDepartment() == null ||
                user.getCompany().getDepartment().trim().isEmpty()) {
            errors.add("company.department es requerido y no puede estar vacío");
        }

        return errors;
    }

    /**
     * Valida formato de email usando regex simple
     */
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    }
}