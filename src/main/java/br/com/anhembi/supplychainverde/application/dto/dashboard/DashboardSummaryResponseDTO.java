package br.com.anhembi.supplychainverde.application.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

/**
 * RN-13c: o mercado recebe suppliers (fornecedores com workedWith) e pendingBatches nulo; a companhia fornecedora
 * recebe pendingBatches (lotes PENDING aguardando ação dela) e suppliers nulo.
 */
public record DashboardSummaryResponseDTO(
        long activeBatches,
        long expiringCertifications,
        Long suppliers,
        Long pendingBatches,
        BigDecimal monthlyEmissionKgCo2e,
        List<RecentBatchSummaryDTO> recentBatches
) {}
