package br.com.anhembi.supplychainverde.application.dto.certification;
import java.time.LocalDate;
public record CertificationRequestDTO(Long supplierId, String certification, String issuingBody, LocalDate issuedAt, LocalDate expiresAt) {}
