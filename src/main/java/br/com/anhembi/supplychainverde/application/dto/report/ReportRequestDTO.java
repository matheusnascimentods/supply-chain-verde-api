package br.com.anhembi.supplychainverde.application.dto.report;
import java.time.LocalDate;
public record ReportRequestDTO(Long supplierId, LocalDate periodStartAt, LocalDate periodEndAt) {}
