package com.challenge.telus.routes;

import com.challenge.telus.models.ApiResponse;
import com.challenge.telus.models.ExtractionState;
import com.challenge.telus.processors.JsonlWriterProcessor;
import com.challenge.telus.processors.PaginationProcessor;
import com.challenge.telus.services.ExtractionStateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Ruta principal para la extracción de usuarios
 * Maneja la paginación, reintentos y persistencia de estado
 */
@Slf4j
@Component
public class UserExtractionRoute extends RouteBuilder {
    
    private final ExtractionStateService stateService;
    private final PaginationProcessor paginationProcessor;
    private final JsonlWriterProcessor jsonlWriterProcessor;
    private final String apiUrl;
    private final Integer limit;
    private final String cronExpression;
    private final Integer maxRetries;
    private final Long initialDelay;
    private final Double multiplier;
    private final Long maxDelay;
    
    public UserExtractionRoute(
            ExtractionStateService stateService,
            PaginationProcessor paginationProcessor,
            JsonlWriterProcessor jsonlWriterProcessor,
            @Value("${api.users.url}") String apiUrl,
            @Value("${api.users.limit}") Integer limit,
            @Value("${extractor.schedule.cron}") String cronExpression,
            @Value("${extractor.error.max-retries}") Integer maxRetries,
            @Value("${extractor.error.initial-delay}") Long initialDelay,
            @Value("${extractor.error.multiplier}") Double multiplier,
            @Value("${extractor.error.max-delay}") Long maxDelay) {
        this.stateService = stateService;
        this.paginationProcessor = paginationProcessor;
        this.jsonlWriterProcessor = jsonlWriterProcessor;
        this.apiUrl = apiUrl;
        this.limit = limit;
        this.cronExpression = cronExpression;
        this.maxRetries = maxRetries;
        this.initialDelay = initialDelay;
        this.multiplier = multiplier;
        this.maxDelay = maxDelay;
    }
    
    @Override
    public void configure() throws Exception {
        
        // Ruta principal programada
        from("quartz://extractionTimer?cron=" + cronExpression)
                .errorHandler(defaultErrorHandler()
                        .maximumRedeliveries(maxRetries)
                        .redeliveryDelay(initialDelay)
                        .backOffMultiplier(multiplier)
                        .maximumRedeliveryDelay(maxDelay)
                        .retryAttemptedLogLevel(LoggingLevel.WARN))
            .routeId("user-extraction-route")
            .log("Iniciando extracción de usuarios programada")
            .process(this::initializeExtraction)
            .to("direct:extract-users")
            .log("Extracción de usuarios completada");
        
        // Ruta de extracción principal
        from("direct:extract-users")
                .errorHandler(defaultErrorHandler()
                        .maximumRedeliveries(maxRetries)
                        .redeliveryDelay(initialDelay)
                        .backOffMultiplier(multiplier)
                        .maximumRedeliveryDelay(maxDelay)
                        .retryAttemptedLogLevel(LoggingLevel.WARN))
            .routeId("extract-users-route")
            .log("Iniciando proceso de extracción de usuarios")
            .process(this::prepareExtraction)
            .loopDoWhile(header("shouldContinue"))
                .to("direct:fetch-page")
                .process(this::handlePageResponse)
            .end()
            .log("Proceso de extracción finalizado");
        
        // Ruta para obtener una página de la API con reintentos
        from("direct:fetch-page")
                .errorHandler(defaultErrorHandler()
                        .maximumRedeliveries(maxRetries)
                        .redeliveryDelay(initialDelay)
                        .backOffMultiplier(multiplier)
                        .maximumRedeliveryDelay(maxDelay)
                        .retryAttemptedLogLevel(LoggingLevel.WARN))
            .routeId("fetch-page-route")
            .log("Obteniendo página de usuarios - Skip: ${header.skip}, Limit: ${header.limit}")
            .setHeader(Exchange.HTTP_METHOD, constant("GET"))
            .setHeader(Exchange.HTTP_URI, simple(apiUrl + "?skip=${header.skip}&limit=${header.limit}"))
            .to("http://dummy")
            .unmarshal().json(JsonLibrary.Jackson, ApiResponse.class)
            .log("Página obtenida exitosamente")
            .onException(Exception.class)
                .log("Error al obtener página - Skip: ${header.skip}, Error: ${exception.message}")
                .handled(true)
                .to("direct:handle-error")
            .end();
        
        // Ruta de error para reintentos
        from("direct:handle-error")
                .errorHandler(defaultErrorHandler()
                        .maximumRedeliveries(maxRetries)
                        .redeliveryDelay(initialDelay)
                        .backOffMultiplier(multiplier)
                        .maximumRedeliveryDelay(maxDelay)
                        .retryAttemptedLogLevel(LoggingLevel.WARN))
            .routeId("handle-error-route")
            .log("Manejando error en extracción: ${exception.message}")
            .process(this::handleExtractionError);
    }
    
    /**
     * Inicializa la extracción verificando si hay una en progreso
     */
    private void initializeExtraction(Exchange exchange) {
        if (stateService.hasInProgressExtraction()) {
            log.info("Se encontró una extracción en progreso, se reanudará");
        } else {
            log.info("Iniciando nueva extracción");
        }
    }
    
    /**
     * Prepara la extracción obteniendo o creando el estado
     */
    private void prepareExtraction(Exchange exchange) {
        // Primero obtenemos el total de registros
        Integer totalRecords = getTotalRecords();
        
        // Obtenemos o creamos el estado
        ExtractionState state = stateService.getOrCreateState(totalRecords, limit);
        exchange.setProperty("extractionState", state);
        
        // Configuramos los parámetros iniciales
        exchange.getMessage().setHeader("skip", state.getLastSuccessfulSkip());
        exchange.getMessage().setHeader("limit", limit);
        exchange.getMessage().setHeader("shouldContinue", true);
        
        log.info("Extracción preparada - Skip inicial: {}, Total: {}", 
                state.getLastSuccessfulSkip(), totalRecords);
    }
    
    /**
     * Maneja la respuesta de una página
     */
    private void handlePageResponse(Exchange exchange) {
        ApiResponse apiResponse = exchange.getIn().getBody(ApiResponse.class);
        ExtractionState state = exchange.getProperty("extractionState", ExtractionState.class);
        
        if (apiResponse == null) {
            log.error("Respuesta de API es null");
            exchange.getMessage().setHeader("shouldContinue", false);
            return;
        }
        
        // Escribir usuarios en JSONL
        if (apiResponse.getUsers() != null && !apiResponse.getUsers().isEmpty()) {
            exchange.getMessage().setBody(apiResponse.getUsers());
            try {
                jsonlWriterProcessor.process(exchange);
            } catch (Exception e) {
                log.error("Error al escribir usuarios en JSONL", e);
                exchange.getMessage().setHeader("shouldContinue", false);
                return;
            }
        }
        
        // Procesar paginación
        exchange.getMessage().setBody(apiResponse);
        try {
            paginationProcessor.process(exchange);
        } catch (Exception e) {
            log.error("Error al procesar paginación", e);
            exchange.getMessage().setHeader("shouldContinue", false);
            return;
        }
        
        // Actualizar estado
        stateService.saveState(state);
        
        // Configurar para la siguiente iteración
        Boolean shouldContinue = exchange.getProperty("shouldContinue", Boolean.class);
        exchange.getMessage().setHeader("shouldContinue", shouldContinue);
        
        if (shouldContinue) {
            exchange.getMessage().setHeader("skip", state.getNextSkip());
            exchange.getMessage().setHeader("limit", limit);
        }
    }
    
    /**
     * Obtiene el total de registros de la API
     */
    private Integer getTotalRecords() {
        try {
            // Hacer una llamada inicial para obtener el total
            Exchange exchange = getContext().createProducerTemplate().request(
                apiUrl + "?skip=0&limit=1", 
                exchange1 -> {
                    exchange1.getIn().setHeader(Exchange.HTTP_METHOD, "GET");
                }
            );
            
            ApiResponse response = exchange.getIn().getBody(ApiResponse.class);
            return response != null ? response.getTotal() : 0;
            
        } catch (Exception e) {
            log.error("Error al obtener el total de registros", e);
            return 0;
        }
    }
    
    /**
     * Maneja errores en la extracción
     */
    private void handleExtractionError(Exchange exchange) {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        log.error("Error en extracción: {}", exception.getMessage(), exception);
    }
}



