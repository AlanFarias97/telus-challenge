package com.challenge.telus.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuración de Jackson para manejo de Java 8 Time API
 * y otras configuraciones globales de serialización
 */
@Configuration
public class JacksonConfig {
    
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Registrar módulo para Java 8 Time API
        mapper.registerModule(new JavaTimeModule());
        
        // Configurar para serializar fechas como ISO-8601
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Configurar para formatear JSON con indentación (deshabilitado para producción)
        // mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        return mapper;
    }
    
    /**
     * DataFormat de Camel para usar el ObjectMapper configurado
     */
    @Bean(name = "jackson")
    public JacksonDataFormat jacksonDataFormat(ObjectMapper objectMapper) {
        JacksonDataFormat dataFormat = new JacksonDataFormat();
        dataFormat.setObjectMapper(objectMapper);
        dataFormat.setPrettyPrint(false); // Más eficiente para archivos grandes
        return dataFormat;
    }
}



