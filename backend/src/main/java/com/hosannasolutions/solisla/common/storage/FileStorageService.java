package com.hosannasolutions.solisla.common.storage;

import org.springframework.web.multipart.MultipartFile;

/** Local filesystem for dev, swappable for an S3-compatible implementation later (design doc §2:
 *  "Do not store product images as database BLOBs"). */
public interface FileStorageService {

    /**
     * Validates content type/size, generates a safe server-side filename (never trusts the
     * original filename — §7), and stores {@code file} under {@code subdirectory}.
     */
    StoredFile store(String subdirectory, MultipartFile file);

    void delete(String storageKey);
}
