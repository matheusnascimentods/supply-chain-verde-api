package br.com.anhembi.supplychainverde.application.usecase.certification;

import br.com.anhembi.supplychainverde.application.dto.certification.CertificationPageResponseDTO;
import br.com.anhembi.supplychainverde.application.mapper.CertificationDtoMapper;
import br.com.anhembi.supplychainverde.domain.repository.CertificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ListCertificationsUseCase {
    private final CertificationRepository certificationRepository;
    private final CertificationDtoMapper mapper;

    public CertificationPageResponseDTO execute(int page, int size, boolean onlyExpiring) {
        LocalDate startsAt = LocalDate.now();
        LocalDate endsAt = startsAt.plusDays(30);
        var certifications = onlyExpiring
                ? certificationRepository.findByExpiresAtBetween(startsAt, endsAt, page, size)
                : certificationRepository.findAll(page, size);
        long totalElements = onlyExpiring
                ? certificationRepository.countByExpiresAtBetween(startsAt, endsAt)
                : certificationRepository.countAll();

        int totalPages = totalElements == 0 ? 0 : Math.toIntExact((totalElements - 1) / size + 1);
        return new CertificationPageResponseDTO(
                certifications.stream().map(mapper::toDto).toList(),
                page,
                size,
                totalElements,
                totalPages
        );
    }
}
