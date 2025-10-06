package com.challenge.telus.repositories;

import com.challenge.telus.entities.ProcessedFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para archivos procesados
 */
@Repository
public interface ProcessedFileRepository extends JpaRepository<ProcessedFileEntity, Long> {
    
    Optional<ProcessedFileEntity> findByFilename(String filename);
    
    boolean existsByFilename(String filename);
}



