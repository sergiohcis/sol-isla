package com.hosannasolutions.solisla.common.storage;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Map<String, String> ALLOWED_CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );
    private static final String PUBLIC_URL_PREFIX = "/media";

    private final Path storageRoot;

    public LocalFileStorageService(@Value("${sol-isla.storage.local-path}") String localPath) {
        this.storageRoot = Path.of(localPath).toAbsolutePath().normalize();
    }

    @PostConstruct
    void ensureStorageRootExists() {
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new FileStorageException("Could not create storage directory: " + storageRoot, e);
        }
    }

    @Override
    public StoredFile store(String subdirectory, MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileStorageException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new FileStorageException("File exceeds the maximum allowed size of 5MB");
        }
        String extension = ALLOWED_CONTENT_TYPE_EXTENSIONS.get(file.getContentType());
        if (extension == null) {
            throw new FileStorageException(
                    "Unsupported file type: " + file.getContentType() + " (allowed: " + Set.of("image/jpeg", "image/png", "image/webp") + ")");
        }

        String filename = UUID.randomUUID() + "." + extension;
        String storageKey = subdirectory + "/" + filename;
        Path targetPath = storageRoot.resolve(storageKey).normalize();
        if (!targetPath.startsWith(storageRoot)) {
            throw new FileStorageException("Invalid storage path");
        }

        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file", e);
        }

        return new StoredFile(storageKey, PUBLIC_URL_PREFIX + "/" + storageKey);
    }

    @Override
    public void delete(String storageKey) {
        Path targetPath = storageRoot.resolve(storageKey).normalize();
        if (!targetPath.startsWith(storageRoot)) {
            throw new FileStorageException("Invalid storage path");
        }
        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            throw new FileStorageException("Failed to delete file", e);
        }
    }
}
