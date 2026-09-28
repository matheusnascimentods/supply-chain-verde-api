package br.com.anhembi.supplychainverde.application.dto.dashboard;

import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;
import br.com.anhembi.supplychainverde.domain.enums.StageType;

import java.math.BigDecimal;

public record RecentBatchSummaryDTO(
        Long batchId,
        String productName,
        String supplierName,
        BigDecimal quantity,
        ProductUnit unit,
        StageType status
) {}
