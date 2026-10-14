package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import java.math.BigDecimal;

public interface MonthlyEmissionProjection {
    String getMonth();

    BigDecimal getCo2Kg();
}
