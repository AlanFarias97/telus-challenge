package com.challenge.telus.processors;

import com.challenge.telus.models.ApiResponse;
import com.challenge.telus.models.ExtractionState;
import com.challenge.telus.services.ExtractionStateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Procesador para manejar la lógica de paginación
 * Determina si debe continuar con la siguiente página o terminar la extracción
 */
@Slf4j
@Component
public class PaginationProcessor implements Processor {
    
    private final ExtractionStateService stateService;
    private final Integer limit;
    
    public PaginationProcessor(
            ExtractionStateService stateService,
            @Value("${api.users.limit:100}") Integer limit) {
        this.stateService = stateService;
        this.limit = limit;
    }
    
    @Override
    public void process(Exchange exchange) throws Exception {
        ApiResponse apiResponse = exchange.getIn().getBody(ApiResponse.class);
        ExtractionState currentState = exchange.getProperty("extractionState", ExtractionState.class);
        
        if (apiResponse == null || currentState == null) {
            log.error("ApiResponse o ExtractionState es null");
            exchange.setProperty("shouldContinue", false);
            return;
        }
        
        log.info("Procesando página - Skip: {}, Limit: {}, Total: {}, Usuarios: {}", 
                apiResponse.getSkip(), apiResponse.getLimit(), apiResponse.getTotal(), 
                apiResponse.getUsers() != null ? apiResponse.getUsers().size() : 0);
        
        // Actualizar el estado con la información de la API
        updateStateFromApiResponse(currentState, apiResponse);
        
        // Determinar si debe continuar
        boolean shouldContinue = determineIfShouldContinue(currentState, apiResponse);
        
        if (shouldContinue) {
            // Preparar la siguiente página
            prepareNextPage(exchange, currentState);
        } else {
            // Marcar como completada
            completeExtraction(currentState);
        }
        
        exchange.setProperty("shouldContinue", shouldContinue);
        exchange.setProperty("extractionState", currentState);
        
        log.info("Decisión de paginación - Continuar: {}, Próximo skip: {}", 
                shouldContinue, shouldContinue ? currentState.getNextSkip() : "N/A");
    }
    
    /**
     * Actualiza el estado basado en la respuesta de la API
     */
    private void updateStateFromApiResponse(ExtractionState state, ApiResponse apiResponse) {
        // Actualizar el total si es la primera página
        if (state.getTotalRecords() == null || state.getTotalRecords() == 0) {
            state.setTotalRecords(apiResponse.getTotal());
        }
        
        // Actualizar el progreso
        int recordsInThisPage = apiResponse.getUsers() != null ? apiResponse.getUsers().size() : 0;
        int newRecordsProcessed = state.getRecordsProcessed() + recordsInThisPage;
        
        state.updateProgress(apiResponse.getSkip(), newRecordsProcessed);
        
        log.debug("Estado actualizado - Skip: {}, Procesados: {}/{}", 
                state.getLastSuccessfulSkip(), state.getRecordsProcessed(), state.getTotalRecords());
    }
    
    /**
     * Determina si debe continuar con la siguiente página
     */
    private boolean determineIfShouldContinue(ExtractionState state, ApiResponse apiResponse) {
        // Si la API dice que no hay más páginas
        if (!apiResponse.hasMorePages()) {
            log.info("API indica que no hay más páginas");
            return false;
        }
        
        // Si hemos procesado todos los registros
        if (state.getRecordsProcessed() >= state.getTotalRecords()) {
            log.info("Se han procesado todos los registros: {}/{}", 
                    state.getRecordsProcessed(), state.getTotalRecords());
            return false;
        }
        
        // Si la página actual está vacía
        if (apiResponse.isEmpty()) {
            log.warn("Página vacía recibida, terminando extracción");
            return false;
        }
        
        return true;
    }
    
    /**
     * Prepara la siguiente página configurando los parámetros de la URL
     */
    private void prepareNextPage(Exchange exchange, ExtractionState state) {
        Integer nextSkip = state.getNextSkip();
        
        // Configurar los headers para la siguiente llamada HTTP
        exchange.getMessage().setHeader("skip", nextSkip);
        exchange.getMessage().setHeader("limit", limit);
        
        log.debug("Preparando siguiente página - Skip: {}, Limit: {}", nextSkip, limit);
    }
    
    /**
     * Marca la extracción como completada
     */
    private void completeExtraction(ExtractionState state) {
        state.markAsCompleted();
        stateService.saveState(state);
        
        log.info("Extracción completada - Total procesados: {}/{}", 
                state.getRecordsProcessed(), state.getTotalRecords());
    }
}
