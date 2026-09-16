package br.com.anhembi.supplychainverde.application.usecase.certification;

import br.com.anhembi.supplychainverde.application.dto.certification.CertificationRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.certification.CertificationResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.entity.Certification;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.enums.CertificationStatus;
import br.com.anhembi.supplychainverde.domain.exception.SupplierNotFoundException;
import br.com.anhembi.supplychainverde.domain.repository.CertificationRepository;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterCertificationUseCase {
    private final CertificationRepository certificationRepository;
    private final SupplierRepository supplierRepository;

    public CertificationResponseDTO execute(Long supplierId, CertificationRequestDTO request) {
        if (request == null) throw new ValidationException("Requisição de certificação é obrigatória.");
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new SupplierNotFoundException(supplierId));

        Certification certification = Certification.builder()
                .supplier(supplier)
                .certification(request.certification())
                .issuingBody(request.issuingBody())
                .issuedAt(request.issuedAt())
                .expiresAt(request.expiresAt())
                .status(CertificationStatus.ACTIVE)
                .build();

        Certification saved = certificationRepository.save(certification);
        return new CertificationResponseDTO(
                saved.getCertificationId(),
                supplierId,
                saved.getCertification(),
                saved.getIssuingBody(),
                saved.getIssuedAt(),
                saved.getExpiresAt(),
                saved.getStatus()
        );
    }

    public CertificationResponseDTO register(Long supplierId, CertificationRequestDTO request) {
        return execute(supplierId, request);
    }
}
