package com.challenge.telus.processors;

import com.challenge.telus.models.InvalidUser;
import com.challenge.telus.models.User;
import com.challenge.telus.models.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Procesador para manejar usuarios inválidos
 * Los guarda en el Dead Letter Queue (DLQ) con información de error
 */
@Slf4j
@Component
public class DeadLetterQueueProcessor implements Processor {

    private final ObjectMapper objectMapper;
    private final String dlqDirectory;
    private final String dlqFilenamePattern;

    public DeadLetterQueueProcessor(
            @Value("${extractor.output.directory:raw_users}") String baseDirectory,
            @Value("${extractor.output.filename-pattern:records_{date:yyyyMMdd_HHmmss}.jsonl}") String filenamePattern) {
        this.objectMapper = new ObjectMapper();
        this.dlqDirectory = baseDirectory + "/dlq";
        this.dlqFilenamePattern = filenamePattern.replace("records_", "invalid_users_");

        // Crear directorio DLQ si no existe
        createDlqDirectoryIfNotExists();
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        ValidationResult validationResult = exchange.getIn().getBody(ValidationResult.class);

        if (validationResult == null || validationResult.isValid()) {
            log.debug("Saltando DLQ - usuario válido o null");
            return;
        }

        User user = validationResult.getUser();
        List<String> errors = validationResult.getErrors();

        log.warn("Procesando usuario inválido {} - Errores: {}",
                user != null ? user.getId() : "null", errors);

        // Crear InvalidUser
        InvalidUser invalidUser = InvalidUser.fromValidationResult(validationResult);

        // Guardar en DLQ usando la ruta configurada en el exchange
        String dlqFilePath = exchange.getProperty("dlqFilePath", String.class);
        if (dlqFilePath != null) {
            saveToDeadLetterQueue(invalidUser, dlqFilePath);
        } else {
            saveToDeadLetterQueue(invalidUser);
        }

        // Incrementar contador de registros inválidos
        incrementInvalidRecords(exchange);

        log.info("Usuario inválido guardado en DLQ: {}", invalidUser.getOriginalUser().getId());
    }

    /**
     * Guarda un usuario inválido en el Dead Letter Queue
     */
    private void saveToDeadLetterQueue(InvalidUser invalidUser) throws IOException {
        String filename = generateDlqFilename();
        File dlqFile = new File(dlqDirectory, filename);

        log.debug("Guardando usuario inválido en DLQ: {}", dlqFile.getAbsolutePath());

        try (FileWriter writer = new FileWriter(dlqFile, true)) { // true para append
            String jsonLine = objectMapper.writeValueAsString(invalidUser);
            writer.write(jsonLine + "\n");
            writer.flush();
        }

        log.debug("Usuario inválido guardado exitosamente en DLQ");
    }

    /**
     * Guarda un usuario inválido en el Dead Letter Queue usando una ruta específica
     */
    private void saveToDeadLetterQueue(InvalidUser invalidUser, String filePath) throws IOException {
        File dlqFile = new File(filePath);
        
        // Crear directorio padre si no existe
        if (dlqFile.getParentFile() != null && !dlqFile.getParentFile().exists()) {
            dlqFile.getParentFile().mkdirs();
        }
        
        log.debug("Guardando usuario inválido en DLQ: {}", dlqFile.getAbsolutePath());

        try (FileWriter writer = new FileWriter(dlqFile, true)) { // true para append
            String jsonLine = objectMapper.writeValueAsString(invalidUser);
            writer.write(jsonLine + "\n");
            writer.flush();
        }

        log.debug("Usuario inválido guardado exitosamente en DLQ");
    }

    /**
     * Incrementa el contador de registros inválidos en el exchange
     */
    private void incrementInvalidRecords(Exchange exchange) {
        Integer invalidRecords = exchange.getProperty("invalidRecords", Integer.class);
        if (invalidRecords != null) {
            exchange.setProperty("invalidRecords", invalidRecords + 1);
        }
        
        Integer totalRecords = exchange.getProperty("totalRecords", Integer.class);
        if (totalRecords != null) {
            exchange.setProperty("totalRecords", totalRecords + 1);
        }
    }

    /**
     * Genera el nombre del archivo DLQ basado en el patrón configurado
     */
    private String generateDlqFilename() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return dlqFilenamePattern.replace("{date:yyyyMMdd_HHmmss}", timestamp);
    }

    /**
     * Crea el directorio DLQ si no existe
     */
    private void createDlqDirectoryIfNotExists() {
        File dlqDir = new File(dlqDirectory);
        if (!dlqDir.exists()) {
            boolean created = dlqDir.mkdirs();
            if (created) {
                log.info("Directorio DLQ creado: {}", dlqDir.getAbsolutePath());
            } else {
                log.error("No se pudo crear el directorio DLQ: {}", dlqDir.getAbsolutePath());
                throw new RuntimeException("No se pudo crear el directorio DLQ");
            }
        }
    }

    /**
     * Procesa múltiples usuarios inválidos de una vez
     */
    public void processBatch(List<ValidationResult> validationResults) {
        for (ValidationResult result : validationResults) {
            if (result != null && result.isInvalid()) {
                try {
                    InvalidUser invalidUser = InvalidUser.fromValidationResult(result);
                    saveToDeadLetterQueue(invalidUser);
                } catch (Exception e) {
                    log.error("Error al procesar usuario inválido en batch", e);
                }
            }
        }
    }
}