package com.challenge.telus.processors;

import com.challenge.telus.models.User;
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
 * Procesador para escribir usuarios en formato JSONL
 * Cada usuario se escribe en una línea separada del archivo
 */
@Slf4j
@Component
public class JsonlWriterProcessor implements Processor {
    
    private final ObjectMapper objectMapper;
    private final String outputDirectory;
    private final String filenamePattern;
    
    public JsonlWriterProcessor(
            @Value("${extractor.output.directory:raw_users}") String outputDirectory,
            @Value("${extractor.output.filename-pattern:records_{date:yyyyMMdd_HHmmss}.jsonl}") String filenamePattern) {
        this.objectMapper = new ObjectMapper();
        this.outputDirectory = outputDirectory;
        this.filenamePattern = filenamePattern;
        
        // Crear directorio de salida si no existe
        createOutputDirectoryIfNotExists();
    }
    
    @Override
    public void process(Exchange exchange) throws Exception {
        List<User> users = exchange.getIn().getBody(List.class);
        
        if (users == null || users.isEmpty()) {
            log.warn("No hay usuarios para escribir en JSONL");
            return;
        }
        
        String filename = generateFilename();
        File outputFile = new File(outputDirectory, filename);
        
        log.info("Escribiendo {} usuarios en archivo: {}", users.size(), outputFile.getAbsolutePath());
        
        try (FileWriter writer = new FileWriter(outputFile, true)) { // true para append
            for (User user : users) {
                String jsonLine = objectMapper.writeValueAsString(user);
                writer.write(jsonLine + "\n");
            }
            writer.flush();
        } catch (IOException e) {
            log.error("Error al escribir archivo JSONL: {}", filename, e);
            throw new RuntimeException("Error al escribir archivo JSONL", e);
        }
        
        log.info("Archivo JSONL actualizado exitosamente: {}", filename);
        
        // Guardar información del archivo en el exchange para uso posterior
        exchange.setProperty("outputFile", outputFile.getAbsolutePath());
        exchange.setProperty("recordsWritten", users.size());
    }
    
    /**
     * Genera el nombre del archivo basado en el patrón configurado
     */
    private String generateFilename() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return filenamePattern.replace("{date:yyyyMMdd_HHmmss}", timestamp);
    }
    
    /**
     * Crea el directorio de salida si no existe
     */
    private void createOutputDirectoryIfNotExists() {
        File outputDir = new File(outputDirectory);
        if (!outputDir.exists()) {
            boolean created = outputDir.mkdirs();
            if (created) {
                log.info("Directorio de salida creado: {}", outputDir.getAbsolutePath());
            } else {
                log.error("No se pudo crear el directorio de salida: {}", outputDir.getAbsolutePath());
                throw new RuntimeException("No se pudo crear el directorio de salida");
            }
        }
    }
}

