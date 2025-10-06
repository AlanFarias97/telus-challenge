package com.challenge.telus.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad para almacenar registros de usuarios inválidos
 * Se guarda en el Dead Letter Queue de la Fase 2
 */
@Entity
@Table(name = "invalid_user_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvalidUserRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "original_data", columnDefinition = "TEXT")
    private String originalData;
    
    @Column(name = "error_reasons", columnDefinition = "TEXT")
    private String errorReasons;
    
    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    @Column(name = "file_path", nullable = false)
    private String filePath;
    
    @Column(name = "error_timestamp", nullable = false)
    private LocalDateTime errorTimestamp;
    
    @Column(name = "processing_date", nullable = false)
    private LocalDateTime processingDate;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ErrorStatus status;
    
    public enum ErrorStatus {
        VALIDATION_ERROR, PROCESSING_ERROR, UNKNOWN_ERROR
    }
}



