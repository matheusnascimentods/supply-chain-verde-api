package br.com.anhembi.supplychainverde.application.usecase.batch;

import br.com.anhembi.supplychainverde.application.dto.batch.BatchResponseDTO;
import br.com.anhembi.supplychainverde.application.mapper.BatchDtoMapper;
import br.com.anhembi.supplychainverde.domain.entity.Batch;
import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import br.com.anhembi.supplychainverde.domain.exception.BatchNotFoundException;
import br.com.anhembi.supplychainverde.domain.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CancelBatchUseCase {
    private final BatchRepository batchRepository;
    private final BatchDtoMapper mapper;

    /** RN-05: só o mercado do lote cancela, e só enquanto PENDING (409 depois). companyType nulo é o PLATFORM_ADMIN (RN-03). */
    @Transactional
    public BatchResponseDTO execute(Long batchId, Long companyId, CompanyType companyType) {
        Batch batch = batchRepository.findById(batchId).orElseThrow(() -> new BatchNotFoundException(batchId));
        if (companyType != null && (companyType != CompanyType.MARKET || !batch.isWithin(companyId, null))) {
            throw new AccessDeniedException("Só o mercado do lote o cancela.");
        }
        batch.cancel();
        return mapper.toResponse(batchRepository.save(batch), null, List.of());
    }
}
