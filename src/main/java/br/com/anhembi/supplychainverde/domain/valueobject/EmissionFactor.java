package br.com.anhembi.supplychainverde.domain.valueobject;

import java.math.BigDecimal;

public record EmissionFactor(BigDecimal value) {

    public static final EmissionFactor ZERO = new EmissionFactor(BigDecimal.ZERO);

    public EmissionFactor {
        if (value == null) {
            throw new IllegalArgumentException("O valor do fator de emissão não pode ser nulo.");
        }

        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O fator de emissão não pode ser negativo.");
        }
    }

    public static EmissionFactor of(double value) {
        return new EmissionFactor(BigDecimal.valueOf(value));
    }

    public static EmissionFactor of(BigDecimal value) {
        return new EmissionFactor(value);
    }

    public BigDecimal multiply(BigDecimal quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("A quantidade para cálculo de emissão não pode ser nula.");
        }
        return this.value.multiply(quantity);
    }

    public BigDecimal multiply(double quantity) {
        return multiply(BigDecimal.valueOf(quantity));
    }
}
