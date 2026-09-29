package br.com.anhembi.supplychainverde.application.dto.audit;

import java.util.List;

public record AuditLogPageDTO(
        List<AuditLogResponseDTO> items,
        int limit,
        int offset,
        boolean hasNext
) {}
