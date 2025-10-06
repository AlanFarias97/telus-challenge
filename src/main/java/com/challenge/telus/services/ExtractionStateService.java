package com.challenge.telus.services;

import com.challenge.telus.models.ExtractionState;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Servicio para manejar el estado de la extracción resumible
 * Persiste el estado en un archivo JSON para poder reanudar la extracción
 */
@Slf4j
@Service
public class ExtractionStateService {
    
    private final ObjectMapper objectMapper;
    private final String stateDirectory;
    private final String stateFileName;
    
    public ExtractionStateService(
            ObjectMapper objectMapper,
            @Value("${extractor.state.directory:state}") String stateDirectory,
            @Value("${extractor.state.file:extraction_state.json}") String stateFileName) {
        this.objectMapper = objectMapper;
        this.stateDirectory = stateDirectory;
        this.stateFileName = stateFileName;
        
        // Crear directorio de estado si no existe
        createStateDirectoryIfNotExists();
    }
    
    /**
     * Guarda el estado de la extracción en archivo
     */
    public void saveState(ExtractionState state) {
        try {
            Path statePath = getStateFilePath();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(statePath.toFile(), state);
            log.debug("Estado de extracción guardado: {}", state);
        } catch (IOException e) {
            log.error("Error al guardar el estado de extracción", e);
            throw new RuntimeException("No se pudo guardar el estado de extracción", e);
        }
    }
    
    /**
     * Carga el estado de la extracción desde archivo
     */
    public ExtractionState loadState() {
        try {
            Path statePath = getStateFilePath();
            if (Files.exists(statePath)) {
                ExtractionState state = objectMapper.readValue(statePath.toFile(), ExtractionState.class);
                log.debug("Estado de extracción cargado: {}", state);
                return state;
            } else {
                log.info("No existe archivo de estado, se creará uno nuevo");
                return null;
            }
        } catch (IOException e) {
            log.error("Error al cargar el estado de extracción", e);
            return null;
        }
    }
    
    /**
     * Elimina el archivo de estado (usado cuando se completa la extracción)
     */
    public void clearState() {
        try {
            Path statePath = getStateFilePath();
            if (Files.exists(statePath)) {
                Files.delete(statePath);
                log.info("Estado de extracción eliminado");
            }
        } catch (IOException e) {
            log.error("Error al eliminar el estado de extracción", e);
        }
    }
    
    /**
     * Verifica si existe un estado de extracción en progreso
     */
    public boolean hasInProgressExtraction() {
        ExtractionState state = loadState();
        return state != null && state.getInProgress() != null && state.getInProgress() && !state.isComplete();
    }
    
    /**
     * Obtiene el estado de extracción en progreso o crea uno nuevo
     */
    public ExtractionState getOrCreateState(Integer totalRecords, Integer limit) {
        ExtractionState state = loadState();
        
        if (state == null || state.isComplete()) {
            log.info("Creando nuevo estado de extracción");
            state = ExtractionState.createInitial(totalRecords, limit);
            saveState(state);
        } else {
            log.info("Reanudando extracción desde skip: {}", state.getLastSuccessfulSkip());
        }
        
        return state;
    }
    
    /**
     * Obtiene la ruta completa del archivo de estado
     */
    private Path getStateFilePath() {
        return Paths.get(stateDirectory, stateFileName);
    }
    
    /**
     * Crea el directorio de estado si no existe
     */
    private void createStateDirectoryIfNotExists() {
        try {
            Path stateDir = Paths.get(stateDirectory);
            if (!Files.exists(stateDir)) {
                Files.createDirectories(stateDir);
                log.info("Directorio de estado creado: {}", stateDir.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Error al crear el directorio de estado", e);
            throw new RuntimeException("No se pudo crear el directorio de estado", e);
        }
    }
}
