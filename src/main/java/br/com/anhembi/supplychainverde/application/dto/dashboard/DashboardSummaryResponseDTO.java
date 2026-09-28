package br.com.anhembi.supplychainverde.application.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponseDTO(
        long activeBatches,
        long expiringCertifications,
        long suppliers,
        BigDecimal monthlyEmissionKgCo2e,
        List<RecentBatchSummaryDTO> recentBatches
) {}
