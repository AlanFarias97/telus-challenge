package com.challenge.telus.utils;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utilidad para encriptación de archivos usando AES-256-GCM
 * Implementación para cumplir con el requisito MANDATORY de encriptación de datos
 */
@Slf4j
public class FileEncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256; // AES-256
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits

    /**
     * Generates a random AES-256 key
     */
    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE);
        return keyGenerator.generateKey();
    }

    /**
     * Converts a key to Base64 format for storage
     */
    public static String keyToString(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * Converts a Base64 string to SecretKey
     */
    public static SecretKey stringToKey(String keyStr) {
        byte[] decodedKey = Base64.getDecoder().decode(keyStr);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
    }

    /**
     * Encrypts a file and returns the encrypted file
     * El archivo encriptado tendrá la extensión .enc
     */
    public static File encryptFile(File inputFile, SecretKey key) throws Exception {
        log.debug("Encrypting file: {}", inputFile.getName());

        // Read original file
        byte[] fileData = Files.readAllBytes(inputFile.toPath());

        // Generate random IV for GCM
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        // Configure cipher
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        // Encrypt
        byte[] encryptedData = cipher.doFinal(fileData);

        // Create encrypted file
        File encryptedFile = new File(inputFile.getParent(), inputFile.getName() + ".enc");

        // Write: IV + encrypted data
        try (FileOutputStream fos = new FileOutputStream(encryptedFile)) {
            fos.write(iv); // First write the IV
            fos.write(encryptedData); // Then write encrypted data
        }

        log.debug("Encrypted file created: {} ({} bytes)", 
                encryptedFile.getName(), encryptedFile.length());

        return encryptedFile;
    }

    /**
     * Decrypts an encrypted file
     */
    public static File decryptFile(File encryptedFile, SecretKey key) throws Exception {
        log.debug("DesEncrypting file: {}", encryptedFile.getName());

        // Read encrypted file
        byte[] fileData = Files.readAllBytes(encryptedFile.toPath());

        // Extract IV (first GCM_IV_LENGTH bytes)
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(fileData, 0, iv, 0, GCM_IV_LENGTH);

        // Extract encrypted data (rest of file)
        byte[] encryptedData = new byte[fileData.length - GCM_IV_LENGTH];
        System.arraycopy(fileData, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);

        // Configure cipher
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        // DesEncrypt
        byte[] decryptedData = cipher.doFinal(encryptedData);

        // Crear archivo desencriptado (quitar extensión .enc)
        String originalName = encryptedFile.getName().replace(".enc", "");
        File decryptedFile = new File(encryptedFile.getParent(), "decrypted_" + originalName);

        // Write decrypted data
        Files.write(decryptedFile.toPath(), decryptedData);

        log.debug("Decrypted file created: {} ({} bytes)", 
                decryptedFile.getName(), decryptedFile.length());

        return decryptedFile;
    }
}
