package com.hosannasolutions.solisla.common.storage;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
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
        // The declared content-type header is client-supplied and trivially spoofable (design
        // doc §51) — verify the actual bytes rather than trusting it.
        if (!looksLikeDeclaredImageType(file)) {
            throw new FileStorageException("File is not a valid " + file.getContentType() + " image");
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

    private boolean looksLikeDeclaredImageType(MultipartFile file) {
        if ("image/webp".equals(file.getContentType())) {
            return hasWebpSignature(file);
        }
        // JPEG/PNG: the JDK's own ImageIO decodes both natively; a non-image (or corrupt) file
        // makes ImageIO.read return null instead of throwing.
        try {
            return ImageIO.read(file.getInputStream()) != null;
        } catch (IOException e) {
            throw new FileStorageException("Could not read uploaded file", e);
        }
    }

    /** A stock JDK's ImageIO has no WebP reader (no third-party plugin dependency has been added
     *  for it), so validate the RIFF/WEBP container signature by hand instead of decoding pixels —
     *  bytes 0-3 "RIFF", bytes 8-11 "WEBP" (https://developers.google.com/speed/webp/docs/riff_container). */
    private boolean hasWebpSignature(MultipartFile file) {
        byte[] header = new byte[12];
        try (var in = file.getInputStream()) {
            if (in.readNBytes(header, 0, header.length) < header.length) {
                return false;
            }
        } catch (IOException e) {
            throw new FileStorageException("Could not read uploaded file", e);
        }
        boolean riff = header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F';
        boolean webp = header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
        return riff && webp;
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
