package br.com.anhembi.supplychainverde.application.dto.certification;

import java.util.List;

public record CertificationPageResponseDTO(
        List<CertificationResponseDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
