package br.com.anhembi.supplychainverde.application.dto.pagination;

import br.com.anhembi.supplychainverde.application.pagination.PageCount;

import java.util.List;

public record OffsetPageResponseDTO<T>(
        List<T> items,
        int limit,
        int offset,
        boolean hasNext,
        int totalPages
) {
    public static <T> OffsetPageResponseDTO<T> of(
            List<T> items,
            int limit,
            int offset,
            long totalElements
    ) {
        boolean hasNext = (long) offset + items.size() < totalElements;
        return new OffsetPageResponseDTO<>(
                items,
                limit,
                offset,
                hasNext,
                PageCount.totalPages(totalElements, limit)
        );
    }
}
