package br.com.anhembi.supplychainverde.application.usecase.certification;

import br.com.anhembi.supplychainverde.application.dto.certification.CertificationResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.certification.UpdateCertificationStatusRequestDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.entity.Certification;
import br.com.anhembi.supplychainverde.domain.repository.CertificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateCertificationStatusUseCase {
    private final CertificationRepository certificationRepository;

    public CertificationResponseDTO execute(Long certificationId, UpdateCertificationStatusRequestDTO request) {
        if (request == null || request.status() == null) {
            throw new ValidationException("Status da certificação é obrigatório.");
        }
        Certification certification = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new IllegalArgumentException("Certificação não encontrada: " + certificationId));
        certification.setStatus(request.status());
        Certification updated = certificationRepository.save(certification);
        return new CertificationResponseDTO(
                updated.getCertificationId(),
                updated.getSupplier() != null ? updated.getSupplier().getSupplierId() : null,
                updated.getCertification(),
                updated.getIssuingBody(),
                updated.getIssuedAt(),
                updated.getExpiresAt(),
                updated.getStatus()
        );
    }
}
