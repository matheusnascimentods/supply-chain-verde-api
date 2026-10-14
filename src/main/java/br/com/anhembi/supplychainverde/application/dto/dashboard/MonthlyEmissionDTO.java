package br.com.anhembi.supplychainverde.application.dto.dashboard;

import java.math.BigDecimal;

public record MonthlyEmissionDTO(String month, BigDecimal co2Kg) {}
