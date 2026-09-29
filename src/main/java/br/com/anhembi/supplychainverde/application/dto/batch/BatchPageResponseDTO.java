package br.com.anhembi.supplychainverde.application.dto.batch;

import java.util.List;

public record BatchPageResponseDTO(
        List<BatchResponseDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
