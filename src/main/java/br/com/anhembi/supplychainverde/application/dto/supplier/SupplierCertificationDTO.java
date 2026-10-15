package br.com.anhembi.supplychainverde.application.dto.supplier;

import br.com.anhembi.supplychainverde.domain.enums.CertificationStatus;

import java.time.LocalDate;

public record SupplierCertificationDTO(
        Long certificationId,
        String certification,
        String issuingBody,
        LocalDate issuedAt,
        LocalDate expiresAt,
        CertificationStatus status
) {}
