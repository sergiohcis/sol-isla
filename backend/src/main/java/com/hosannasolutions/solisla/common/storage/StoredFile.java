package com.hosannasolutions.solisla.common.storage;

/** {@code storageKey} is the path relative to the storage root (kept so a future S3-compatible
 *  {@link FileStorageService} can delete/re-derive it); {@code publicUrl} is what callers persist
 *  and serve to clients. */
public record StoredFile(String storageKey, String publicUrl) {
}
