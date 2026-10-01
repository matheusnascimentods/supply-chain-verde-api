package br.com.anhembi.supplychainverde.application.dto.product;

import java.util.List;

public record ProductPageDTO(
        List<ProductResponseDTO> items,
        int limit,
        int offset,
        boolean hasNext,
        int totalPages
) {}
