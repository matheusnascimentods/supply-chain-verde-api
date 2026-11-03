package br.com.anhembi.supplychainverde.application.usecase.chain;

import org.springframework.security.access.AccessDeniedException;
import br.com.anhembi.supplychainverde.domain.exception.BatchStatusConflictException;
import br.com.anhembi.supplychainverde.domain.enums.BatchStatus;
import br.com.anhembi.supplychainverde.application.dto.chain.ChainRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.chain.ChainResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.entity.Address;
import br.com.anhembi.supplychainverde.domain.entity.Batch;
import br.com.anhembi.supplychainverde.domain.entity.Chain;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.StageType;
import br.com.anhembi.supplychainverde.domain.exception.InvalidStageTransitionException;
import br.com.anhembi.supplychainverde.domain.repository.AddressRepository;
import br.com.anhembi.supplychainverde.domain.repository.BatchRepository;
import br.com.anhembi.supplychainverde.domain.repository.ChainRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.application.mapper.ChainDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class RegisterChainStageUseCase {
    private final ChainRepository chainRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ChainDtoMapper mapper;

    @Transactional
    /** marketId e supplierId são o escopo de quem escreve; os dois nulos são o PLATFORM_ADMIN. */
    public ChainResponseDTO execute(Long batchId, Long responsibleUserId, ChainRequestDTO request, Long marketId, Long supplierId) {
        if (request == null) throw new ValidationException("Requisição de etapa é obrigatória.");
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Lote não encontrado: " + batchId));
        if (!batch.isWithin(marketId, supplierId)) throw new AccessDeniedException("Lote de outra companhia.");
        if (batch.getStatus() == BatchStatus.REJECTED) throw new BatchStatusConflictException(batchId, batch.getStatus());
        // RN-18: em PENDING só o fornecedor do lote começa a cadeia, com PRODUCTION.
        boolean pending = batch.getStatus() == BatchStatus.PENDING;
        if (pending && supplierId == null) throw new AccessDeniedException("Só o fornecedor inicia a produção do lote.");
        if (request.stageType() == null) throw new ValidationException("Tipo da etapa é obrigatório.");
        StageType current = chainRepository.findByBatchId(batchId).stream()
                .map(Chain::getStageType)
                .max(Comparator.naturalOrder())
                .orElse(null);
        StageType expected = current == null ? StageType.PRODUCTION : current.next();
        if (request.stageType() != expected) {
            throw new InvalidStageTransitionException(current, request.stageType());
        }
        User responsibleUser = userRepository.findById(responsibleUserId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário responsável não encontrado: " + responsibleUserId));
        // O responsável do header precisa ser do mercado ou do fornecedor do lote.
        Long responsibleCompany = responsibleUser.getCompanyId();
        if (responsibleCompany == null || !(responsibleCompany.equals(batch.getMarketId())
                || responsibleCompany.equals(batch.getSupplier().getCompanyId()))) {
            throw new AccessDeniedException("O responsável não é do mercado nem do fornecedor do lote.");
        }
        if (pending) {
            if (request.startedAt() == null) throw new ValidationException("Data de início da etapa é obrigatória.");
            batch.activate(request.startedAt().toLocalDate());
        }

        Address origin = request.originAddressId() != null ? addressRepository.findById(request.originAddressId()).orElse(null) : null;
        Address destination = request.destinationAddressId() != null ? addressRepository.findById(request.destinationAddressId()).orElse(null) : null;

        Chain stage = Chain.builder()
                .batch(batch)
                .originAddress(origin)
                .destinationAddress(destination)
                .responsibleUser(responsibleUser)
                .stageType(request.stageType())
                .startedAt(request.startedAt())
                .endedAt(request.endedAt())
                .build();

        ChainResponseDTO saved = mapper.toResponse(chainRepository.save(stage), null, null);
        if (pending) batchRepository.save(batch);
        return saved;
    }
}
