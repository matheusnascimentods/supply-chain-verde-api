package br.com.anhembi.supplychainverde.application.usecase.emission;

import br.com.anhembi.supplychainverde.application.dto.carbonemission.CarbonEmissionResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.carbonemission.CarbonFootprintResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.CarbonEmission;
import br.com.anhembi.supplychainverde.domain.repository.CarbonEmissionRepository;
import br.com.anhembi.supplychainverde.application.mapper.CarbonEmissionDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetBatchCarbonFootprintUseCase {
    private final CarbonEmissionRepository carbonEmissionRepository;
    private final CarbonEmissionDtoMapper mapper;

    public CarbonFootprintResponseDTO execute(Long batchId) {
        List<CarbonEmission> emissions = carbonEmissionRepository.findByBatchId(batchId);
        BigDecimal total = emissions.stream().map(CarbonEmission::getCo2Kg).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<CarbonEmissionResponseDTO> items = emissions.stream().map(mapper::toResponse).toList();
        return new CarbonFootprintResponseDTO(batchId, total, items);
    }
}
