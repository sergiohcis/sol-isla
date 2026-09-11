package com.hosannasolutions.solisla.catalog;

/** {@code ARCHIVED} rather than a hard delete once a product has historical order references
 *  (design doc §32 / CLAUDE.md rule 10: ACTIVE -> INACTIVE -> ARCHIVED). */
public enum ProductStatus {
    DRAFT,
    ACTIVE,
    INACTIVE,
    ARCHIVED
}
