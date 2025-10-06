package com.challenge.telus.routes;

import com.challenge.telus.entities.ProcessedFileEntity;
import com.challenge.telus.models.FileProcessedMessage;
import com.challenge.telus.repositories.ProcessedFileRepository;
import com.challenge.telus.utils.FileEncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.File;
import java.time.LocalDateTime;

/**
 * Kafka Consumer - Phase 3
 * Listens for processed user messages and:
 * 1. Saves metadata to SQLite
 * 2. Uploads files to SFTP with encryption and SSH keys
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "spring.application.name", havingValue = "telus-consumer")
public class KafkaConsumerRoute extends RouteBuilder {

    @Autowired
    private ProcessedFileRepository processedFileRepository;

    @Autowired
    @Qualifier("jackson")
    private JacksonDataFormat jacksonDataFormat;

    @Value("${kafka.bootstrap-servers}")
    private String kafkaBootstrapServers;

    @Value("${kafka.topics.processed-users}")
    private String topic;

    @Value("${kafka.consumer.group-id}")
    private String groupId;

    @Value("${sftp.host}")
    private String sftpHost;

    @Value("${sftp.port}")
    private Integer sftpPort;

    @Value("${sftp.username}")
    private String sftpUsername;

    @Value("${sftp.password:#{null}}")
    private String sftpPassword;

    @Value("${sftp.private-key-path:#{null}}")
    private String sftpPrivateKeyPath;

    @Value("${sftp.directory}")
    private String sftpDirectory;

    @Value("${sftp.enabled:true}")
    private Boolean sftpEnabled;

    @Value("${sftp.encryption.enabled:true}")
    private Boolean encryptionEnabled;

    @Value("${sftp.encryption.key}")
    private String encryptionKey;

    private SecretKey secretKey;

    @Override
    public void configure() throws Exception {

        // Initialize encryption key if enabled
        if (encryptionEnabled && encryptionKey != null && !encryptionKey.isEmpty()) {
            try {
                secretKey = FileEncryptionUtil.stringToKey(encryptionKey);
                log.info("File encryption enabled (AES-256-GCM)");
            } catch (Exception e) {
                log.error("Failed to initialize encryption key", e);
                throw new RuntimeException("Encryption initialization failed", e);
            }
        } else {
            log.warn("File encryption DISABLED - DOES NOT MEET MANDATORY REQUIREMENTS");
        }

        // Configure error handler
        errorHandler(defaultErrorHandler()
                .maximumRedeliveries(3)
                .redeliveryDelay(5000)
                .backOffMultiplier(2)
                .retryAttemptedLogLevel(LoggingLevel.WARN));

        // Main route: Consume from Kafka
        from("kafka:" + topic + 
             "?brokers=" + kafkaBootstrapServers +
             "&groupId=" + groupId +
             "&autoOffsetReset=earliest" +
             "&autoCommitEnable=false" +
             "&allowManualCommit=true")
                .routeId("kafka-consumer-route")
                .log("Message received from Kafka: ${body}")
                .process(exchange -> {
                    try {
                        String json = exchange.getIn().getBody(String.class);
                        FileProcessedMessage message = jacksonDataFormat.getObjectMapper().readValue(json, FileProcessedMessage.class);
                        exchange.getIn().setBody(message);
                    } catch (Exception e) {
                        log.error("Failed to deserialize file message from Kafka", e);
                        throw new RuntimeException("Deserialization failed", e);
                    }
                })
                .log("Message deserialized: File=${body.sourceFile}, Valid records=${body.validRecords}")
                .setProperty("sourceFile", simple("${body.sourceFile}"))
                // Process files: save to DB and upload to SFTP
                .to("direct:process-files")
                .log("Files processed completely: ${exchangeProperty.sourceFile}");

        // Route to process files (3 files: raw, processed, dlq)
        from("direct:process-files")
                .routeId("process-files-route")
                .log("Processing files from message")
                .process(this::saveFilesToDatabase)
                .choice()
                    .when(simple("${properties:sftp.enabled} == true"))
                        .log("SFTP enabled, uploading files")
                        .to("direct:upload-files-to-sftp")
                    .otherwise()
                        .log("SFTP disabled, skipping upload")
                .end();

        // Route to upload files to SFTP
        from("direct:upload-files-to-sftp")
                .routeId("upload-files-to-sftp-route")
                .log("Uploading files to SFTP")
                .process(this::uploadFilesToSftp)
                .log("Files uploaded successfully to SFTP");
    }

    /**
     * Saves metadata of processed files to the database
     */
    private void saveFilesToDatabase(Exchange exchange) {
        FileProcessedMessage message = exchange.getIn().getBody(FileProcessedMessage.class);

        if (message == null) {
            log.warn("Message is null, cannot save metadata");
            return;
        }

        try {
            // Save metadata for raw file
            saveFileMetadata(message.getRawFilePath(), message.getTotalRecords(), message.getSourceFile());
            
            // Save metadata for processed file
            if (message.getValidRecords() > 0) {
                saveFileMetadata(message.getProcessedFilePath(), message.getValidRecords(), message.getSourceFile());
            }
            
            // Guardar metadato del archivo DLQ si hay registros inválidos
            if (message.getInvalidRecords() > 0) {
                saveFileMetadata(message.getDlqFilePath(), message.getInvalidRecords(), message.getSourceFile());
            }

            log.info("Metadata saved to DB for file: {}", message.getSourceFile());

        } catch (Exception e) {
            log.error("Failed to save metadata to DB", e);
            throw new RuntimeException("Failed to save metadata to DB", e);
        }
    }

    /**
     * Saves metadata for an individual file to the database
     */
    private void saveFileMetadata(String filePath, int totalRecords, String sourceFile) {
        if (filePath == null || filePath.isEmpty()) {
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            log.warn("Archivo no existe, no se guardará metadata: {}", filePath);
            return;
        }

        ProcessedFileEntity entity = new ProcessedFileEntity();
        entity.setFilename(file.getName());
        entity.setFilePath(filePath);
        entity.setTotalRecords(totalRecords);
        entity.setSourceFile(sourceFile);
        entity.setProcessingDate(LocalDateTime.now());
        entity.setUploadedToSftp(false);

        processedFileRepository.save(entity);
        
        log.debug("Metadata saved for file: {}", file.getName());
    }

    /**
     * Uploads processed files to SFTP
     */
    private void uploadFilesToSftp(Exchange exchange) {
        FileProcessedMessage message = exchange.getIn().getBody(FileProcessedMessage.class);

        if (message == null) {
            log.warn("Message is null, cannot upload files");
            return;
        }

        try {
            // Upload raw file
            uploadFileToSftp(message.getRawFilePath());
            
            // Upload processed file if exists
            if (message.getValidRecords() > 0) {
                uploadFileToSftp(message.getProcessedFilePath());
            }
            
            // Upload DLQ file if exists
            if (message.getInvalidRecords() > 0) {
                uploadFileToSftp(message.getDlqFilePath());
            }

            // Update status in DB
            updateSftpUploadStatus(message);

            log.info("Files uploaded to SFTP for: {}", message.getSourceFile());

        } catch (Exception e) {
            log.error("Failed to upload files to SFTP", e);
            throw new RuntimeException("Failed to upload files to SFTP", e);
        }
    }

    /**
     * Uploads an individual file to SFTP using SSH Key Authentication
     * y encriptación AES-256-GCM (MANDATORY)
     */
    private void uploadFileToSftp(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            log.warn("Archivo no existe, no se subirá al SFTP: {}", filePath);
            return;
        }

        File fileToUpload = file;
        File encryptedFile = null;

        try {
            // Encriptar archivo si está habilitado (MANDATORY)
            if (encryptionEnabled && secretKey != null) {
                encryptedFile = FileEncryptionUtil.encryptFile(file, secretKey);
                fileToUpload = encryptedFile;
                log.debug("File encrypted: {} → {}", file.getName(), encryptedFile.getName());
            } else {
                log.warn("Uploading file UNENCRYPTED: {} - DOES NOT MEET REQUIREMENTS", file.getName());
            }

            // Construir URL con SSH Key o Password según configuración
            String sftpUrl;
            if (sftpPrivateKeyPath != null && !sftpPrivateKeyPath.isEmpty()) {
                // Autenticación con SSH Key (MANDATORY)
                sftpUrl = String.format("sftp://%s:%d/%s?username=%s&privateKeyFile=%s&passiveMode=true&autoCreate=true&knownHostsFile=/dev/null&strictHostKeyChecking=no",
                        sftpHost, sftpPort, sftpDirectory, sftpUsername, sftpPrivateKeyPath);
                log.debug("Using SSH Key Authentication: {}", sftpPrivateKeyPath);
            } else if (sftpPassword != null && !sftpPassword.isEmpty()) {
                // Fallback to password if no private key
                sftpUrl = String.format("sftp://%s:%d/%s?username=%s&password=%s&passiveMode=true&autoCreate=true&knownHostsFile=/dev/null&strictHostKeyChecking=no",
                        sftpHost, sftpPort, sftpDirectory, sftpUsername, sftpPassword);
                log.warn("Using Password Authentication (not recommended)");
            } else {
                throw new IllegalStateException("No se configuró ni clave privada ni password para SFTP");
            }

            getContext().createProducerTemplate()
                    .sendBodyAndHeader(sftpUrl, fileToUpload, Exchange.FILE_NAME, fileToUpload.getName());

            log.debug("File uploaded to SFTP: {}", fileToUpload.getName());

            // Limpiar File encrypted temporal
            if (encryptedFile != null && encryptedFile.exists()) {
                encryptedFile.delete();
                log.debug("Temporary encrypted file deleted: {}", encryptedFile.getName());
            }

        } catch (Exception e) {
            log.error("Failed to upload file {} al SFTP", file.getName(), e);
            // Limpiar File encrypted temporal en caso de error
            if (encryptedFile != null && encryptedFile.exists()) {
                encryptedFile.delete();
            }
            throw new RuntimeException("Failed to upload file a SFTP", e);
        }
    }

    /**
     * Updates SFTP upload status in the database
     */
    private void updateSftpUploadStatus(FileProcessedMessage message) {
        processedFileRepository.findAll().stream()
                .filter(entity -> entity.getSourceFile().equals(message.getSourceFile()))
                .forEach(entity -> {
                    entity.setUploadedToSftp(true);
                    entity.setUploadedToSftpAt(LocalDateTime.now());
                    processedFileRepository.save(entity);
                });
    }
}



