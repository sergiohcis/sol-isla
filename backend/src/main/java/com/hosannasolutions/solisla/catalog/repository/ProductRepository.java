package com.hosannasolutions.solisla.catalog.repository;

import com.hosannasolutions.solisla.catalog.Product;
import com.hosannasolutions.solisla.catalog.ProductStatus;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    boolean existsBySku(String sku);

    boolean existsBySlug(String slug);

    Optional<Product> findBySlug(String slug);

    /**
     * Backs both the public catalog (caller always passes {@code status = ACTIVE}) and the admin
     * product list ({@code status = null} for "all statuses") — see design doc §11's search
     * filters. PostgreSQL {@code ILIKE} substring matching is enough at this catalog's expected
     * scale; revisit with trigram/full-text indexes only if it becomes a real bottleneck (§11).
     * <p>
     * {@code q} must be {@code ""} rather than {@code null} for "no search term" — the caller
     * (ProductServiceImpl) guarantees this. A NULL bind used only inside {@code lower(concat(...))}
     * has no column to infer its type from, and PostgreSQL defaults an untyped NULL parameter to
     * bytea in that position, so {@code lower(:q)} fails with "function lower(bytea) does not
     * exist" for every search with no query text — confirmed against the running dev server.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE (:status IS NULL OR p.status = :status)
              AND (:categoryId IS NULL OR p.categoryId = :categoryId)
              AND (:q = '' OR lower(p.name) LIKE lower(concat('%', :q, '%'))
                           OR lower(p.sku) LIKE lower(concat('%', :q, '%'))
                           OR lower(p.description) LIKE lower(concat('%', :q, '%')))
              AND (:minPrice IS NULL OR p.basePrice >= :minPrice)
              AND (:maxPrice IS NULL OR p.basePrice <= :maxPrice)
              AND (:featured IS NULL OR p.featured = :featured)
            """)
    Page<Product> search(
            @Param("status") ProductStatus status,
            @Param("categoryId") UUID categoryId,
            @Param("q") String q,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("featured") Boolean featured,
            Pageable pageable);
}
