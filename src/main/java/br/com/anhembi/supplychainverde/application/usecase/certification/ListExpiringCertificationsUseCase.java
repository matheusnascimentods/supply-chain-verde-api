package br.com.anhembi.supplychainverde.application.usecase.certification;

import br.com.anhembi.supplychainverde.application.dto.certification.CertificationResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Certification;
import br.com.anhembi.supplychainverde.domain.repository.CertificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ListExpiringCertificationsUseCase {
    private final CertificationRepository certificationRepository;

    public List<CertificationResponseDTO> execute() {
        LocalDate now = LocalDate.now();
        return certificationRepository.findByExpiresAtBetween(now, now.plusDays(30)).stream()
                .map(this::toResponse).toList();
    }

    private CertificationResponseDTO toResponse(Certification certification) {
        return new CertificationResponseDTO(
                certification.getCertificationId(),
                certification.getSupplier() != null ? certification.getSupplier().getSupplierId() : null,
                certification.getCertification(),
                certification.getIssuingBody(),
                certification.getIssuedAt(),
                certification.getExpiresAt(),
                certification.getStatus()
        );
    }
}
