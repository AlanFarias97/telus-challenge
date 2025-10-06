package com.challenge.telus.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad para almacenar metadatos de archivos procesados
 */
@Entity
@Table(name = "processed_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false, length = 500)
    private String filePath;

    @Column(nullable = false)
    private Integer totalRecords;

    @Column(nullable = false)
    private String sourceFile;

    @Column(nullable = false)
    private LocalDateTime processingDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime uploadedToSftpAt;

    @Column
    private Boolean uploadedToSftp = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (processingDate == null) {
            processingDate = LocalDateTime.now();
        }
    }
}



