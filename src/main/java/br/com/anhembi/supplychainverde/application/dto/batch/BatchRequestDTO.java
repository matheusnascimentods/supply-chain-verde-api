package br.com.anhembi.supplychainverde.application.dto.batch;
import java.math.BigDecimal;
import java.time.LocalDate;
public record BatchRequestDTO(Long productId, Long supplierId, BigDecimal quantity, LocalDate producedAt) {}
