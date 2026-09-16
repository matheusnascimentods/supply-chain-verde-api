package br.com.anhembi.supplychainverde.application.dto.batch;
import java.math.BigDecimal;
import java.time.LocalDate;
public record BatchResponseDTO(Long batchId, Long productId, String productName, Long supplierId, String supplierName, BigDecimal quantity, LocalDate producedAt) {}
