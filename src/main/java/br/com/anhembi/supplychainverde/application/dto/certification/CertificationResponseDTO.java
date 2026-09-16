package br.com.anhembi.supplychainverde.application.dto.certification;
import java.time.LocalDate;
import br.com.anhembi.supplychainverde.domain.enums.CertificationStatus;
public record CertificationResponseDTO(Long certificationId, Long supplierId, String certification, String issuingBody, LocalDate issuedAt, LocalDate expiresAt, CertificationStatus status) {}
