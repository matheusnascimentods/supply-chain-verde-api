package br.com.anhembi.supplychainverde.application.dto.supplier;
import java.math.BigDecimal;
public record SupplierRankingDTO(Long supplierId, String name, BigDecimal sustainabilityScore, Integer activeCertificationCount, BigDecimal totalCo2Kg) {}
