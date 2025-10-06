package com.challenge.telus.controllers;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para disparar manualmente la extracción de usuarios
 * Útil para testing sin esperar al cron
 */
@Slf4j
@RestController
@RequestMapping("/api/extraction")
public class ExtractionController {
    
    @Autowired
    private ProducerTemplate producerTemplate;
    
    /**
     * Endpoint para disparar manualmente la extracción
     * POST http://localhost:8080/api/extraction/trigger
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerExtraction() {
        log.info("Disparando extracción manual de usuarios");
        
        try {
            // Disparar la ruta de extracción
            producerTemplate.sendBody("direct:extract-users", "");
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Extracción iniciada exitosamente");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            log.info("Extracción manual completada exitosamente");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al disparar extracción manual", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Error al iniciar extracción: " + e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Endpoint para verificar el estado de la extracción
     * GET http://localhost:8080/api/extraction/status
     */
    @RequestMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        log.info("Consultando estado de extracción");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "available");
        response.put("message", "Sistema de extracción operativo");
        response.put("timestamp", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
}



