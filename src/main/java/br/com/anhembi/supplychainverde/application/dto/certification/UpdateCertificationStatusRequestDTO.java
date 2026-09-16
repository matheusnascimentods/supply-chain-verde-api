package br.com.anhembi.supplychainverde.application.dto.certification;
import br.com.anhembi.supplychainverde.domain.enums.CertificationStatus;
public record UpdateCertificationStatusRequestDTO(CertificationStatus status) {}
