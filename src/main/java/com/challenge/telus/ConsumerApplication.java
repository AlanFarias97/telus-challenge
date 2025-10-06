package com.challenge.telus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aplicación Consumer - Fase 3
 * Escucha mensajes de Kafka y guarda en DB/SFTP
 * 
 * Este es un deployment independiente que:
 * 1. Consume mensajes de Kafka (usuarios procesados)
 * 2. Guarda metadatos en SQLite
 * 3. Sube archivos al SFTP
 */
@SpringBootApplication
public class ConsumerApplication {

    public static void main(String[] args) {
        // Verificar si estamos en modo consumer
        String appName = System.getProperty("spring.application.name", "");
        
        if ("telus-consumer".equals(appName) || args.length > 0 && "consumer".equals(args[0])) {
            System.out.println("=================================================");
            System.out.println("  TELUS CONSUMER - Fase 3 (DB + SFTP)");
            System.out.println("=================================================");
            SpringApplication.run(ConsumerApplication.class, args);
        }
    }
}



