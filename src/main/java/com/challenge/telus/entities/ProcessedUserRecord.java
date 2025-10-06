package com.challenge.telus.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad para almacenar registros de usuarios procesados
 * Se guarda cuando se completa la transformación de la Fase 2
 */
@Entity
@Table(name = "processed_user_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedUserRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "first_name", nullable = false)
    private String firstName;
    
    @Column(name = "last_name")
    private String lastName;
    
    @Column(name = "email", nullable = false)
    private String email;
    
    @Column(name = "age")
    private Integer age;
    
    @Column(name = "department")
    private String department;
    
    @Column(name = "department_code")
    private String departmentCode;
    
    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    @Column(name = "file_path", nullable = false)
    private String filePath;
    
    @Column(name = "processing_date", nullable = false)
    private LocalDateTime processingDate;

    @JsonProperty("insertion_date")
    @Column(name = "insertion_date", nullable = false)
    private LocalDateTime insertionDate;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProcessingStatus status;
    
    public enum ProcessingStatus {
        VALID, INVALID, PROCESSED
    }
}



