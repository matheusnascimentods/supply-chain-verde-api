package br.com.anhembi.supplychainverde.application.usecase.certification;

import br.com.anhembi.supplychainverde.application.dto.certification.CertificationPageResponseDTO;
import br.com.anhembi.supplychainverde.application.PageCount;
import br.com.anhembi.supplychainverde.application.mapper.CertificationDtoMapper;
import br.com.anhembi.supplychainverde.domain.enums.CertificationStatus;
import br.com.anhembi.supplychainverde.domain.repository.CertificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListCertificationsUseCase {
    private final CertificationRepository certificationRepository;
    private final CertificationDtoMapper mapper;

    public CertificationPageResponseDTO execute(int page, int size, CertificationStatus status) {
        var certifications = status == null
                ? certificationRepository.findAll(page, size)
                : certificationRepository.findByStatus(status, page, size);
        long totalElements = status == null
                ? certificationRepository.countAll()
                : certificationRepository.countByStatus(status);

        int totalPages = PageCount.totalPages(totalElements, size);
        return new CertificationPageResponseDTO(
                certifications.stream().map(mapper::toDto).toList(),
                page,
                size,
                totalElements,
                totalPages
        );
    }
}
