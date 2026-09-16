package br.com.anhembi.supplychainverde.application.dto.report;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record ReportResponseDTO(Long reportId, Long supplierId, LocalDate periodStartAt, LocalDate periodEndAt, BigDecimal totalCo2Kg, Integer trackedProductCount, LocalDateTime generatedAt) {}
