package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import java.math.BigDecimal;

public interface DashboardBatchProjection {
    Long getBatchId();

    String getProductName();

    String getSupplierName();

    BigDecimal getQuantity();

    String getUnit();

    String getStatus();
}
