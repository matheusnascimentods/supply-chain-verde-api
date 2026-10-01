package br.com.anhembi.supplychainverde.application.dto.supplier;

import java.util.List;

public record SupplierRankingPageDTO(
        List<SupplierRankingDTO> items,
        int limit,
        int offset,
        boolean hasNext,
        int totalPages
) {}
