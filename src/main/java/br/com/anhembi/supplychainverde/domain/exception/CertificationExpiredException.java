package br.com.anhembi.supplychainverde.domain.exception;

public class CertificationExpiredException extends DomainException {
    public CertificationExpiredException(Long certificationId) {
        super("Certificação expirada: " + certificationId);
    }
}
