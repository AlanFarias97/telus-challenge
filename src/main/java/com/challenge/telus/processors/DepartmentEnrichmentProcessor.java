package com.challenge.telus.processors;

import com.challenge.telus.models.DepartmentMapping;
import com.challenge.telus.models.User;
import com.challenge.telus.models.ValidatedUser;
import com.challenge.telus.models.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Procesador para enriquecer usuarios con códigos de departamento
 * Lee el CSV de departamentos y mapea nombres a códigos
 */
@Slf4j
@Component
public class DepartmentEnrichmentProcessor implements Processor {

    private final ObjectMapper objectMapper;
    private final Resource departmentsCsvResource;
    private Map<String, String> departmentMappings;

    public DepartmentEnrichmentProcessor(
            @Value("classpath:data/departments.csv") Resource departmentsCsvResource) {
        this.objectMapper = new ObjectMapper();
        this.departmentsCsvResource = departmentsCsvResource;
        this.departmentMappings = new HashMap<>();
    }

    @PostConstruct
    public void loadDepartmentMappings() {
        try {
            loadDepartmentsFromCsv();
            log.info("Cargados {} mapeos de departamentos", departmentMappings.size());
        } catch (Exception e) {
            log.error("Error al cargar mapeos de departamentos", e);
            throw new RuntimeException("No se pudieron cargar los mapeos de departamentos", e);
        }
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        ValidationResult validationResult = exchange.getIn().getBody(ValidationResult.class);

        if (validationResult == null || !validationResult.isValid()) {
            log.debug("Saltando enriquecimiento - usuario inválido");
            exchange.getMessage().setBody(validationResult);
            return;
        }

        User user = validationResult.getUser();
        log.debug("Enriqueciendo usuario {} con departamento: {}",
                user.getId(), user.getCompany().getDepartment());

        ValidatedUser enrichedUser = enrichUser(user);
        exchange.getMessage().setBody(enrichedUser);
    }

    /**
     * Enriquece un usuario con el código de departamento
     */
    private ValidatedUser enrichUser(User user) {
        ValidatedUser enrichedUser = new ValidatedUser();

        // Copiar campos básicos
        enrichedUser.setId(user.getId());
        enrichedUser.setFirstName(user.getFirstName());
        enrichedUser.setLastName(user.getLastName());
        enrichedUser.setEmail(user.getEmail());
        enrichedUser.setPhone(user.getPhone());
        enrichedUser.setUsername(user.getUsername());
        enrichedUser.setBirthDate(user.getBirthDate());
        enrichedUser.setImage(user.getImage());

        // Copiar objetos anidados
        enrichedUser.setAddress(mapAddress(user.getAddress()));
        enrichedUser.setCompany(mapCompany(user.getCompany()));
        enrichedUser.setBank(mapBank(user.getBank()));

        // Enriquecer con código de departamento
        String departmentName = user.getCompany().getDepartment();
        String departmentCode = departmentMappings.getOrDefault(departmentName, "UNK");
        enrichedUser.setDepartmentCode(departmentCode);

        // Agregar fecha de inserción
        enrichedUser.setInsertionDate(LocalDateTime.now());

        log.debug("Usuario {} enriquecido - Departamento: {} -> {}",
                user.getId(), departmentName, departmentCode);

        return enrichedUser;
    }

    /**
     * Carga los mapeos de departamentos desde el archivo CSV
     */
    private void loadDepartmentsFromCsv() throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(departmentsCsvResource.getInputStream()))) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                // Saltar la primera línea (headers)
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String departmentName = parts[0].trim();
                    String departmentCode = parts[1].trim();
                    departmentMappings.put(departmentName, departmentCode);

                    log.debug("Mapeo cargado: {} -> {}", departmentName, departmentCode);
                }
            }
        }
    }

    /**
     * Obtiene el código de departamento para un nombre dado
     */
    public String getDepartmentCode(String departmentName) {
        return departmentMappings.getOrDefault(departmentName, "UNK");
    }

    /**
     * Verifica si existe un mapeo para un departamento
     */
    public boolean hasDepartmentMapping(String departmentName) {
        return departmentMappings.containsKey(departmentName);
    }

    /**
     * Mapea Address de User a ValidatedUser
     */
    private ValidatedUser.Address mapAddress(User.Address userAddress) {
        if (userAddress == null) return null;

        ValidatedUser.Address validatedAddress = new ValidatedUser.Address();
        validatedAddress.setAddress(userAddress.getAddress());
        validatedAddress.setCity(userAddress.getCity());
        validatedAddress.setPostalCode(userAddress.getPostalCode());
        validatedAddress.setState(userAddress.getState());

        if (userAddress.getCoordinates() != null) {
            ValidatedUser.Address.Coordinates coords = new ValidatedUser.Address.Coordinates();
            coords.setLat(userAddress.getCoordinates().getLat());
            coords.setLng(userAddress.getCoordinates().getLng());
            validatedAddress.setCoordinates(coords);
        }

        return validatedAddress;
    }

    /**
     * Mapea Company de User a ValidatedUser
     */
    private ValidatedUser.Company mapCompany(User.Company userCompany) {
        if (userCompany == null) return null;

        ValidatedUser.Company validatedCompany = new ValidatedUser.Company();
        validatedCompany.setName(userCompany.getName());
        validatedCompany.setTitle(userCompany.getTitle());
        validatedCompany.setDepartment(userCompany.getDepartment());
        validatedCompany.setAddress(mapAddress(userCompany.getAddress()));

        return validatedCompany;
    }

    /**
     * Mapea Bank de User a ValidatedUser
     */
    private ValidatedUser.Bank mapBank(User.Bank userBank) {
        if (userBank == null) return null;

        ValidatedUser.Bank validatedBank = new ValidatedUser.Bank();
        validatedBank.setCardExpire(userBank.getCardExpire());
        validatedBank.setCardNumber(userBank.getCardNumber());
        validatedBank.setCardType(userBank.getCardType());
        validatedBank.setCurrency(userBank.getCurrency());
        validatedBank.setIban(userBank.getIban());

        return validatedBank;
    }
}