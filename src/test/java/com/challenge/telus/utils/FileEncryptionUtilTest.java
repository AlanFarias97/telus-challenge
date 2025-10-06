package com.challenge.telus.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.crypto.SecretKey;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FileEncryptionUtil
 * Tests AES-256-GCM encryption and decryption functionality
 */
class FileEncryptionUtilTest {

    @TempDir
    Path tempDir;

    private SecretKey secretKey;

    @BeforeEach
    void setUp() throws Exception {
        // Generate a test encryption key
        secretKey = FileEncryptionUtil.generateKey();
    }

    @Test
    void testGenerateKey() throws Exception {
        // Given & When
        SecretKey key = FileEncryptionUtil.generateKey();

        // Then
        assertNotNull(key, "Generated key should not be null");
        assertEquals("AES", key.getAlgorithm(), "Key algorithm should be AES");
        assertEquals(32, key.getEncoded().length, "Key should be 256 bits (32 bytes)");
    }

    @Test
    void testKeyToStringAndBack() throws Exception {
        // Given
        SecretKey originalKey = FileEncryptionUtil.generateKey();

        // When
        String keyString = FileEncryptionUtil.keyToString(originalKey);
        SecretKey restoredKey = FileEncryptionUtil.stringToKey(keyString);

        // Then
        assertNotNull(keyString, "Key string should not be null");
        assertNotNull(restoredKey, "Restored key should not be null");
        assertArrayEquals(originalKey.getEncoded(), restoredKey.getEncoded(),
                "Restored key should match original key");
    }

    @Test
    void testEncryptFile() throws Exception {
        // Given
        File originalFile = createTestFile("test-data.jsonl", "Test content for encryption");

        // When
        File encryptedFile = FileEncryptionUtil.encryptFile(originalFile, secretKey);

        // Then
        assertNotNull(encryptedFile, "Encrypted file should not be null");
        assertTrue(encryptedFile.exists(), "Encrypted file should exist");
        assertTrue(encryptedFile.getName().endsWith(".enc"), "Encrypted file should have .enc extension");
        assertTrue(encryptedFile.length() > 0, "Encrypted file should not be empty");
        
        // Verify encrypted content is different from original
        byte[] originalContent = Files.readAllBytes(originalFile.toPath());
        byte[] encryptedContent = Files.readAllBytes(encryptedFile.toPath());
        assertNotEquals(originalContent.length, encryptedContent.length,
                "Encrypted file should have different size (IV + encrypted data + tag)");

        // Cleanup
        encryptedFile.delete();
    }

    @Test
    void testEncryptAndDecryptFile() throws Exception {
        // Given
        String originalContent = "This is sensitive user data that needs encryption!";
        File originalFile = createTestFile("sensitive-data.jsonl", originalContent);

        // When - Encrypt
        File encryptedFile = FileEncryptionUtil.encryptFile(originalFile, secretKey);
        
        // When - Decrypt
        File decryptedFile = FileEncryptionUtil.decryptFile(encryptedFile, secretKey);

        // Then
        assertNotNull(decryptedFile, "Decrypted file should not be null");
        assertTrue(decryptedFile.exists(), "Decrypted file should exist");
        
        String decryptedContent = Files.readString(decryptedFile.toPath());
        assertEquals(originalContent, decryptedContent, "Decrypted content should match original");

        // Cleanup
        encryptedFile.delete();
        decryptedFile.delete();
    }

    @Test
    void testEncryptionWithDifferentKeys() throws Exception {
        // Given
        File originalFile = createTestFile("data.jsonl", "Test data");
        SecretKey key1 = FileEncryptionUtil.generateKey();
        SecretKey key2 = FileEncryptionUtil.generateKey();

        // When
        File encrypted1 = FileEncryptionUtil.encryptFile(originalFile, key1);
        File encrypted2 = FileEncryptionUtil.encryptFile(originalFile, key2);

        byte[] content1 = Files.readAllBytes(encrypted1.toPath());
        byte[] content2 = Files.readAllBytes(encrypted2.toPath());

        // Then
        assertFalse(java.util.Arrays.equals(content1, content2),
                "Same file encrypted with different keys should produce different results");

        // Cleanup
        encrypted1.delete();
        encrypted2.delete();
    }

    @Test
    void testDecryptWithWrongKey() throws Exception {
        // Given
        File originalFile = createTestFile("secret.jsonl", "Secret data");
        SecretKey correctKey = FileEncryptionUtil.generateKey();
        SecretKey wrongKey = FileEncryptionUtil.generateKey();

        // When
        File encryptedFile = FileEncryptionUtil.encryptFile(originalFile, correctKey);

        // Then
        assertThrows(Exception.class, () -> {
            FileEncryptionUtil.decryptFile(encryptedFile, wrongKey);
        }, "Decryption with wrong key should throw exception");

        // Cleanup
        encryptedFile.delete();
    }

    @Test
    void testEncryptLargeFile() throws Exception {
        // Given - Create a larger file (1KB)
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largeContent.append("Line ").append(i).append(": Some test data for encryption\n");
        }
        File largeFile = createTestFile("large-data.jsonl", largeContent.toString());

        // When
        File encryptedFile = FileEncryptionUtil.encryptFile(largeFile, secretKey);
        File decryptedFile = FileEncryptionUtil.decryptFile(encryptedFile, secretKey);

        // Then
        String originalContent = Files.readString(largeFile.toPath());
        String decryptedContent = Files.readString(decryptedFile.toPath());
        assertEquals(originalContent, decryptedContent, "Large file should decrypt correctly");

        // Cleanup
        encryptedFile.delete();
        decryptedFile.delete();
    }

    /**
     * Helper method to create test files
     */
    private File createTestFile(String filename, String content) throws Exception {
        Path filePath = tempDir.resolve(filename);
        Files.writeString(filePath, content);
        return filePath.toFile();
    }
}

