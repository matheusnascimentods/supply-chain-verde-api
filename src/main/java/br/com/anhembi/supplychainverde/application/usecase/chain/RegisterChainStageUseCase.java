package br.com.anhembi.supplychainverde.application.usecase.chain;

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
    public ChainResponseDTO execute(Long batchId, Long responsibleUserId, ChainRequestDTO request) {
        if (request == null) throw new ValidationException("Requisição de etapa é obrigatória.");
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Lote não encontrado: " + batchId));
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

        return mapper.toResponse(chainRepository.save(stage), null, null);
    }
}
