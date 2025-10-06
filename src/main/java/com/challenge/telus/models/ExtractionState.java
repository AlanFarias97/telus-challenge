package com.challenge.telus.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Modelo para manejar el estado de la extracción resumible
 * Se persiste en archivo JSON para poder reanudar la extracción
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionState {
    
    /**
     * Último skip exitoso procesado
     */
    private Integer lastSuccessfulSkip;
    
    /**
     * Total de registros a extraer
     */
    private Integer totalRecords;
    
    /**
     * Límite de registros por página
     */
    private Integer limit;
    
    /**
     * Timestamp de la última actualización
     */
    @JsonProperty("lastUpdated")
    private LocalDateTime lastUpdated;
    
    /**
     * Indica si la extracción está en progreso
     */
    private Boolean inProgress;
    
    /**
     * Indica si la extracción se completó exitosamente
     */
    private Boolean completed;
    
    /**
     * Número de registros procesados exitosamente
     */
    private Integer recordsProcessed;
    
    /**
     * Timestamp de inicio de la extracción
     */
    @JsonProperty("startTime")
    private LocalDateTime startTime;
    
    /**
     * Timestamp de finalización de la extracción
     */
    @JsonProperty("endTime")
    private LocalDateTime endTime;
    
    /**
     * Crea un estado inicial para una nueva extracción
     */
    public static ExtractionState createInitial(Integer totalRecords, Integer limit) {
        ExtractionState state = new ExtractionState();
        state.setLastSuccessfulSkip(0);
        state.setTotalRecords(totalRecords);
        state.setLimit(limit);
        state.setInProgress(true);
        state.setCompleted(false);
        state.setRecordsProcessed(0);
        state.setStartTime(LocalDateTime.now());
        state.setLastUpdated(LocalDateTime.now());
        return state;
    }
    
    /**
     * Verifica si la extracción está completa
     */
    @JsonIgnore
    public boolean isComplete() {
        return completed != null && completed;
    }
    
    /**
     * Verifica si hay más páginas por procesar
     */
    @JsonIgnore
    public boolean hasMorePages() {
        return lastSuccessfulSkip + limit < totalRecords;
    }
    
    /**
     * Obtiene el siguiente valor de skip
     */
    @JsonIgnore
    public Integer getNextSkip() {
        return lastSuccessfulSkip + limit;
    }
    
    /**
     * Marca la extracción como completada
     */
    public void markAsCompleted() {
        this.completed = true;
        this.inProgress = false;
        this.endTime = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }
    
    /**
     * Actualiza el progreso después de procesar una página exitosamente
     */
    public void updateProgress(Integer skip, Integer recordsProcessed) {
        this.lastSuccessfulSkip = skip;
        this.recordsProcessed = recordsProcessed;
        this.lastUpdated = LocalDateTime.now();
    }
}
