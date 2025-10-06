package com.challenge.telus.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Mensaje que se envía a Kafka cuando se completa el procesamiento de un archivo
 * Contiene las rutas de los archivos generados (raw, processed, dlq)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileProcessedMessage {
    
    @JsonProperty("sourceFile")
    private String sourceFile;
    
    @JsonProperty("rawFilePath")
    private String rawFilePath;
    
    @JsonProperty("processedFilePath")
    private String processedFilePath;
    
    @JsonProperty("dlqFilePath")
    private String dlqFilePath;
    
    @JsonProperty("totalRecords")
    private Integer totalRecords;
    
    @JsonProperty("validRecords")
    private Integer validRecords;
    
    @JsonProperty("invalidRecords")
    private Integer invalidRecords;
    
    @JsonProperty("processingDate")
    private LocalDateTime processingDate;
}


