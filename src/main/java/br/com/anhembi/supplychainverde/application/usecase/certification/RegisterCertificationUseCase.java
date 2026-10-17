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
import br.com.anhembi.supplychainverde.application.mapper.CertificationDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RegisterCertificationUseCase {
    private final CertificationRepository certificationRepository;
    private final SupplierRepository supplierRepository;
    private final CertificationDtoMapper mapper;

    @Transactional
    public CertificationResponseDTO execute(Long supplierId, CertificationRequestDTO request) {
        if (request == null) throw new ValidationException("Requisição de certificação é obrigatória.");
        if (request.expiresAt() != null && request.expiresAt().isBefore(LocalDate.now())) {
            throw new ValidationException("Não é possível cadastrar uma certificação já vencida.");
        }
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new SupplierNotFoundException(supplierId));

        Certification certification = mapper.toDomain(request);
        certification.setSupplier(supplier);
        certification.setStatus(CertificationStatus.ACTIVE);
        return mapper.toResponse(certificationRepository.save(certification));
    }
}
