package br.com.anhembi.supplychainverde.application.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReportListItemDTO(
        Long reportId,
        Long supplierId,
        String supplierCnpj,
        String supplierName,
        LocalDate periodStartAt,
        LocalDate periodEndAt,
        BigDecimal totalCo2Kg,
        long totalBatchCount,
        LocalDateTime generatedAt
) {}
