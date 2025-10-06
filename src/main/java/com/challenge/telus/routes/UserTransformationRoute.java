package com.challenge.telus.routes;

import com.challenge.telus.models.FileProcessedMessage;
import com.challenge.telus.models.User;
import com.challenge.telus.models.ValidatedUser;
import com.challenge.telus.models.ValidationResult;
import com.challenge.telus.processors.DeadLetterQueueProcessor;
import com.challenge.telus.processors.DepartmentEnrichmentProcessor;
import com.challenge.telus.processors.UserValidationProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;

/**
 * Ruta principal para la transformación y validación de usuarios
 * Se activa cuando aparece un nuevo archivo JSONL en raw_users/
 * Procesa línea por línea, valida, enriquece y guarda resultados
 */
@Slf4j
@Component
public class UserTransformationRoute extends RouteBuilder {

    private final UserValidationProcessor userValidationProcessor;
    private final DepartmentEnrichmentProcessor departmentEnrichmentProcessor;
    private final DeadLetterQueueProcessor deadLetterQueueProcessor;
    private final ObjectMapper objectMapper;
    private final JacksonDataFormat jacksonDataFormat;
    private final String rawUsersDirectory;
    private final String processedUsersDirectory;

    public UserTransformationRoute(
            UserValidationProcessor userValidationProcessor,
            DepartmentEnrichmentProcessor departmentEnrichmentProcessor,
            DeadLetterQueueProcessor deadLetterQueueProcessor,
            ObjectMapper objectMapper,
            @Qualifier("jackson") JacksonDataFormat jacksonDataFormat,
            @Value("${extractor.output.directory:raw_users}") String rawUsersDirectory,
            @Value("${transformation.output.processed-directory:processed_users}") String processedUsersDirectory) {
        this.userValidationProcessor = userValidationProcessor;
        this.departmentEnrichmentProcessor = departmentEnrichmentProcessor;
        this.deadLetterQueueProcessor = deadLetterQueueProcessor;
        this.objectMapper = objectMapper;
        this.jacksonDataFormat = jacksonDataFormat;
        this.rawUsersDirectory = rawUsersDirectory;
        this.processedUsersDirectory = processedUsersDirectory;

        // Crear directorio de usuarios procesados si no existe
        createProcessedUsersDirectoryIfNotExists();
    }

    @Override
    public void configure() throws Exception {

        // Ruta principal: Monitorear archivos JSONL en raw_users/
        from("file:" + rawUsersDirectory + "?include=.*\\.jsonl&move=.done&moveFailed=.error&initialDelay=5000&delay=30000")
                .routeId("user-transformation-route")
                .log("Archivo JSONL detectado: ${file:name}")
                .process(this::logFileInfo)
                .process(this::initializeProcessedFileFromInput)
                .setProperty("validRecords", constant(0))
                .setProperty("invalidRecords", constant(0))
                .setProperty("totalRecords", constant(0))
                .to("direct:process-jsonl-file")
                .process(this::sendFileProcessedMessage)
                .log("Procesamiento de archivo completado: ${file:name}");

        // Ruta para procesar archivo JSONL línea por línea
        from("direct:process-jsonl-file")
                .routeId("process-jsonl-file-route")
                .log("Iniciando procesamiento línea por línea")
                .split().tokenize("\n")
                .streaming()
                .process(this::logLineProcessing)
                .to("direct:process-user-line")
                .end()
                .log("Procesamiento de todas las líneas completado");

        // Ruta para procesar una línea individual
        from("direct:process-user-line")
                .routeId("process-user-line-route")
                .errorHandler(defaultErrorHandler()
                        .maximumRedeliveries(2)
                        .redeliveryDelay(1000)
                        .retryAttemptedLogLevel(LoggingLevel.WARN))
                .log("Procesando línea: ${body}")
                .process(this::parseUserFromLine)
                .choice()
                .when(body().isNotNull())
                .to("direct:validate-user")
                .otherwise()
                .log("Línea vacía o inválida, saltando")
                .end();

        // Ruta para validar usuario
        from("direct:validate-user")
                .routeId("validate-user-route")
                .log("Validando usuario: ${body.id}")
                .process(userValidationProcessor)
                .choice()
                .when(body().isNotNull())
                .to("direct:enrich-user")
                .otherwise()
                .log("Usuario inválido, enviando a DLQ")
                .process(deadLetterQueueProcessor)
                .end();

        // Ruta para enriquecer usuario
        from("direct:enrich-user")
                .routeId("enrich-user-route")
                .log("Enriqueciendo usuario: ${body.user.id}")
                .process(departmentEnrichmentProcessor)
                .choice()
                .when(body().isNotNull())
                .to("direct:save-validated-user")
                .otherwise()
                .log("Error en enriquecimiento, enviando a DLQ")
                .process(deadLetterQueueProcessor)
                .end();

        // Ruta para guardar usuario validado
        from("direct:save-validated-user")
                .routeId("save-validated-user-route")
                .log("Guardando usuario validado: ${body.id}")
                .process(this::saveValidatedUserToFile)
                .process(this::incrementValidRecords)
                .log("Usuario validado guardado exitosamente");
        
        // Ruta para enviar mensaje de archivo completo a Kafka (Fase 3)
        from("direct:send-file-to-kafka")
                .routeId("kafka-producer-route")
                .log("Enviando información de archivo procesado a Kafka")
                .marshal(jacksonDataFormat)
                .to("kafka:{{kafka.topics.processed-users}}?brokers={{kafka.bootstrap-servers}}")
                .log("Archivo procesado enviado a Kafka: ${body}");
    }

    /**
     * Registra información del archivo que se está procesando
     */
    private void logFileInfo(Exchange exchange) {
        String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
        Long fileSize = exchange.getIn().getHeader("CamelFileLength", Long.class);
        log.info("Procesando archivo: {} ({} bytes)", fileName, fileSize);
    }

    /**
     * Registra el procesamiento de cada línea
     */
    private void logLineProcessing(Exchange exchange) {
        Integer lineNumber = exchange.getIn().getHeader("CamelSplitIndex", Integer.class);
        String line = exchange.getIn().getBody(String.class);
        log.debug("Procesando línea {}: {}", lineNumber, line != null ? line.substring(0, Math.min(50, line.length())) + "..." : "null");
    }

    /**
     * Parsea un usuario desde una línea JSON
     */
    private void parseUserFromLine(Exchange exchange) {
        String line = exchange.getIn().getBody(String.class);

        if (line == null || line.trim().isEmpty()) {
            log.debug("Línea vacía, saltando");
            exchange.getMessage().setBody(null);
            return;
        }

        try {
            // Parsear JSON a User
            User user = objectMapper.readValue(line, User.class);
            exchange.getMessage().setBody(user);
            exchange.getMessage().setHeader("originalLine", line);
        } catch (Exception e) {
            log.error("Error al parsear línea JSON: {}", line, e);
            exchange.getMessage().setBody(null);
        }
    }

    /**
     * Inicializa el archivo de usuarios procesados basado en el archivo de entrada
     */
    private void initializeProcessedFileFromInput(Exchange exchange) {
        String inputFileName = exchange.getIn().getHeader("CamelFileName", String.class);
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        // Generar nombre de archivo basado en el input
        String baseName = inputFileName != null ? inputFileName.replace(".jsonl", "") : "records";
        String outputFileName = "etl_" + baseName + "_" + timestamp + ".jsonl";
        String outputFilePath = processedUsersDirectory + "/" + outputFileName;
        
        String dlqFileName = "invalid_" + baseName + "_" + timestamp + ".jsonl";
        String dlqFilePath = processedUsersDirectory + "/../dlq/" + dlqFileName;

        exchange.setProperty("processedFileName", outputFileName);
        exchange.setProperty("processedFilePath", outputFilePath);
        exchange.setProperty("dlqFileName", dlqFileName);
        exchange.setProperty("dlqFilePath", dlqFilePath);

        log.info("Archivo de salida inicializado: {} (entrada: {})", outputFileName, inputFileName);
    }

    /**
     * Guarda un usuario validado en el archivo de salida
     */
    private void saveValidatedUserToFile(Exchange exchange) {
        ValidatedUser validatedUser = exchange.getIn().getBody(ValidatedUser.class);

        if (validatedUser == null) {
            log.warn("Usuario validado es null, saltando guardado");
            return;
        }

        String filePath = exchange.getProperty("processedFilePath", String.class);
        if (filePath == null) {
            log.error("Ruta de archivo procesado no encontrada");
            return;
        }

        try {
            // Crear archivo si no existe
            File file = new File(filePath);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            // Escribir usuario en formato JSONL (usar objectMapper inyectado)
            try (java.io.FileWriter writer = new java.io.FileWriter(file, true)) {
                String jsonLine = objectMapper.writeValueAsString(validatedUser);
                writer.write(jsonLine + "\n");
                writer.flush();
            }

            log.debug("Usuario {} guardado en archivo procesado", validatedUser.getId());

        } catch (Exception e) {
            log.error("Error al guardar usuario validado", e);
            throw new RuntimeException("Error al guardar usuario validado", e);
        }
    }

    /**
     * Crea el directorio de usuarios procesados si no existe
     */
    private void createProcessedUsersDirectoryIfNotExists() {
        File processedDir = new File(processedUsersDirectory);
        if (!processedDir.exists()) {
            boolean created = processedDir.mkdirs();
            if (created) {
                log.info("Directorio de usuarios procesados creado: {}", processedDir.getAbsolutePath());
            } else {
                log.error("No se pudo crear el directorio de usuarios procesados: {}", processedDir.getAbsolutePath());
                throw new RuntimeException("No se pudo crear el directorio de usuarios procesados");
            }
        }
    }

    /**
     * Incrementa el contador de registros válidos
     */
    private void incrementValidRecords(Exchange exchange) {
        Integer validRecords = exchange.getProperty("validRecords", Integer.class);
        if (validRecords != null) {
            exchange.setProperty("validRecords", validRecords + 1);
        }
        
        Integer totalRecords = exchange.getProperty("totalRecords", Integer.class);
        if (totalRecords != null) {
            exchange.setProperty("totalRecords", totalRecords + 1);
        }
    }

    /**
     * Envía un mensaje a Kafka con la información del archivo procesado completo
     */
    private void sendFileProcessedMessage(Exchange exchange) {
        String sourceFile = exchange.getIn().getHeader("CamelFileName", String.class);
        String processedFilePath = exchange.getProperty("processedFilePath", String.class);
        String dlqFilePath = exchange.getProperty("dlqFilePath", String.class);

        // Construir ruta del archivo raw (en .done)
        String rawFilePath = rawUsersDirectory + "/.done/" + sourceFile;

        // Contar registros leyendo los archivos generados
        int validRecords = countLinesInFile(processedFilePath);
        int invalidRecords = countLinesInFile(dlqFilePath);
        int totalRecords = validRecords + invalidRecords;

        // Crear mensaje con toda la información del archivo
        FileProcessedMessage message = new FileProcessedMessage();
        message.setSourceFile(sourceFile);
        message.setRawFilePath(rawFilePath);
        message.setProcessedFilePath(processedFilePath);
        message.setDlqFilePath(dlqFilePath);
        message.setTotalRecords(totalRecords);
        message.setValidRecords(validRecords);
        message.setInvalidRecords(invalidRecords);
        message.setProcessingDate(LocalDateTime.now());

        log.info("Archivo procesado: {} → {} válidos, {} inválidos, {} total", 
                sourceFile, validRecords, invalidRecords, totalRecords);

        // Establecer el mensaje como body para enviarlo a Kafka
        exchange.getIn().setBody(message);
        
        // Enviar a Kafka
        try {
            getContext().createProducerTemplate().send("direct:send-file-to-kafka", exchange);
        } catch (Exception e) {
            log.error("Failed to send file message to Kafka", e);
        }
    }

    /**
     * Cuenta las líneas de un archivo
     */
    private int countLinesInFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return 0;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            return 0;
        }

        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
            int lines = 0;
            while (reader.readLine() != null) {
                lines++;
            }
            return lines;
        } catch (Exception e) {
            log.error("Error al contar líneas del archivo {}", filePath, e);
            return 0;
        }
    }
}
