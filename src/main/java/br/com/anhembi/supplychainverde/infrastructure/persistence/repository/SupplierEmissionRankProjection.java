package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import java.math.BigDecimal;

public interface SupplierEmissionRankProjection {
    Long getSupplierId();

    BigDecimal getCo2KgPerUnit();
}
