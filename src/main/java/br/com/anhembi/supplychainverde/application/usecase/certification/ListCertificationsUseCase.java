package br.com.anhembi.supplychainverde.application.usecase.certification;

import br.com.anhembi.supplychainverde.application.dto.certification.CertificationPageResponseDTO;
import br.com.anhembi.supplychainverde.application.pagination.PageCount;
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

    /** RN-12: certificações são catálogo para o mercado; a companhia fornecedora (supplierId) vê só as dela. */
    public CertificationPageResponseDTO execute(int page, int size, CertificationStatus status, Long supplierId) {
        var certifications = certificationRepository.findPage(status, supplierId, page, size);
        long totalElements = certificationRepository.countPage(status, supplierId);

        int totalPages = PageCount.totalPages(totalElements, size);
        return new CertificationPageResponseDTO(
                certifications.stream().map(mapper::toResponse).toList(),
                page,
                size,
                totalElements,
                totalPages
        );
    }
}
