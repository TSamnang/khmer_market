package com.example.khmer_market.service;

import jakarta.annotation.PostConstruct;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${market.storage.upload-dir:uploads}")
    private String uploadDirProperty;

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final Tika tika = new Tika();
    private Path rootStorageLocation;

    @PostConstruct
    public void init() {
        try {
            rootStorageLocation = Paths.get(uploadDirProperty).toAbsolutePath().normalize();
            if (!Files.exists(rootStorageLocation)) {
                Files.createDirectories(rootStorageLocation);
            }
            log.info("Initialized secure file storage at: {}", rootStorageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize file storage directory", e);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store an empty file");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 5MB limit");
        }

        // Real MIME type verification using Apache Tika magic bytes
        String detectedMimeType;
        try (InputStream inputStream = file.getInputStream()) {
            detectedMimeType = tika.detect(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to inspect uploaded file content", e);
        }

        if (!ALLOWED_MIME_TYPES.contains(detectedMimeType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type: " + detectedMimeType + ". Only JPEG, PNG, and WebP are permitted.");
        }

        String extension = switch (detectedMimeType.toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };

        // Cryptographically random UUID filename to prevent collisions and predict attacks
        String filename = UUID.randomUUID().toString() + extension;
        Path destinationFile = rootStorageLocation.resolve(filename).normalize().toAbsolutePath();

        // Path Traversal Defense: Ensure destination is strictly within rootStorageLocation
        if (!destinationFile.startsWith(rootStorageLocation)) {
            throw new SecurityException("Path traversal attempt detected");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("Securely stored upload: {}", filename);
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public Path loadFileAsPath(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename cannot be blank");
        }

        Path resolved = rootStorageLocation.resolve(filename).normalize().toAbsolutePath();

        // Path Traversal check
        if (!resolved.startsWith(rootStorageLocation)) {
            throw new SecurityException("Path traversal attempt detected: " + filename);
        }

        if (!Files.exists(resolved) || !Files.isReadable(resolved)) {
            throw new IllegalArgumentException("File not found or unreadable: " + filename);
        }

        return resolved;
    }

    public void deleteFile(String filename) {
        if (filename == null || filename.isBlank()) return;
        try {
            Path file = rootStorageLocation.resolve(filename).normalize().toAbsolutePath();
            if (file.startsWith(rootStorageLocation)) {
                Files.deleteIfExists(file);
                log.info("Deleted upload file: {}", filename);
            }
        } catch (IOException e) {
            log.warn("Could not delete upload file: {}", filename, e);
        }
    }

    public String storeBytes(byte[] bytes, String extension) {
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }
        String filename = UUID.randomUUID().toString() + extension;
        Path destinationFile = rootStorageLocation.resolve(filename).normalize().toAbsolutePath();
        if (!destinationFile.startsWith(rootStorageLocation)) {
            throw new SecurityException("Path traversal attempt detected");
        }
        try {
            Files.write(destinationFile, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store bytes", e);
        }
    }
}
