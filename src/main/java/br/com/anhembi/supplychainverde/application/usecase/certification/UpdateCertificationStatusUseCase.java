package br.com.anhembi.supplychainverde.application.usecase.certification;

import org.springframework.security.access.AccessDeniedException;
import br.com.anhembi.supplychainverde.application.dto.certification.CertificationResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.certification.UpdateCertificationStatusRequestDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.entity.Certification;
import br.com.anhembi.supplychainverde.domain.repository.CertificationRepository;
import br.com.anhembi.supplychainverde.application.mapper.CertificationDtoMapper;
import br.com.anhembi.supplychainverde.domain.enums.CertificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UpdateCertificationStatusUseCase {
    private final CertificationRepository certificationRepository;
    private final CertificationDtoMapper mapper;

    @Transactional
    /** RN-22: só o fornecedor dono muda o status; ownSupplierId nulo só passa para o PLATFORM_ADMIN. */
    public CertificationResponseDTO execute(Long certificationId, UpdateCertificationStatusRequestDTO request,
                                            Long ownSupplierId, boolean platformAdmin) {
        if (request == null || request.status() == null) {
            throw new ValidationException("Status da certificação é obrigatório.");
        }
        Certification certification = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new IllegalArgumentException("Certificação não encontrada: " + certificationId));
        if (!platformAdmin && (ownSupplierId == null || !ownSupplierId.equals(certification.getSupplier().getCompanyId()))) {
            throw new AccessDeniedException("Só o fornecedor dono altera a certificação.");
        }
        if (request.status() == CertificationStatus.ACTIVE && certification.getExpiresAt().isBefore(LocalDate.now())) {
            throw new ValidationException("Certificação vencida não pode voltar a ficar ativa.");
        }
        certification.setStatus(request.status());
        return mapper.toResponse(certificationRepository.save(certification));
    }
}
