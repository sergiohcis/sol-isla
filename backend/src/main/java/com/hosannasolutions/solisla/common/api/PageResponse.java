package com.hosannasolutions.solisla.common.api;

import java.util.List;
import org.springframework.data.domain.Page;

/** Stable API envelope for paginated results, so callers never bind directly to Spring Data's
 *  {@link Page} (§34 DTO strategy: don't leak persistence types through REST). */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    public static <S, T> PageResponse<T> of(Page<S> page, List<T> mappedItems) {
        return new PageResponse<>(
                mappedItems,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
