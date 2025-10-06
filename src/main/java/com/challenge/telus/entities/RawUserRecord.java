package com.challenge.telus.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad para almacenar registros de usuarios raw
 * Se guarda cuando se completa la extracción de la Fase 1
 */
@Entity
@Table(name = "raw_user_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RawUserRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    @Column(name = "file_path", nullable = false)
    private String filePath;
    
    @Column(name = "total_records")
    private Integer totalRecords;
    
    @Column(name = "processed_records")
    private Integer processedRecords;
    
    @Column(name = "extraction_date", nullable = false)
    private LocalDateTime extractionDate;
    
    @Column(name = "completion_date")
    private LocalDateTime completionDate;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ExtractionStatus status;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    public enum ExtractionStatus {
        IN_PROGRESS, COMPLETED, FAILED
    }
}



