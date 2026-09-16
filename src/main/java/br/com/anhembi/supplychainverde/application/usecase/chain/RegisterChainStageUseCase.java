package br.com.anhembi.supplychainverde.application.usecase.chain;

import br.com.anhembi.supplychainverde.application.dto.address.AddressResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.chain.ChainRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.chain.ChainResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.entity.Address;
import br.com.anhembi.supplychainverde.domain.entity.Batch;
import br.com.anhembi.supplychainverde.domain.entity.Chain;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.repository.AddressRepository;
import br.com.anhembi.supplychainverde.domain.repository.BatchRepository;
import br.com.anhembi.supplychainverde.domain.repository.ChainRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterChainStageUseCase {
    private final ChainRepository chainRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    public ChainResponseDTO execute(Long batchId, Long responsibleUserId, ChainRequestDTO request) {
        if (request == null) throw new ValidationException("Requisição de etapa é obrigatória.");
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Lote não encontrado: " + batchId));
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

        Chain saved = chainRepository.save(stage);
        return new ChainResponseDTO(
                saved.getChainId(),
                batchId,
                toAddressResponse(origin),
                toAddressResponse(destination),
                responsibleUser.getUserId(),
                responsibleUser.getName(),
                saved.getStageType(),
                saved.getStartedAt(),
                saved.getEndedAt(),
                null,
                null
        );
    }

    private AddressResponseDTO toAddressResponse(Address address) {
        if (address == null) return null;
        return new AddressResponseDTO(
                address.getAddressId(),
                address.getStreet(),
                address.getNumber(),
                address.getNeighborhood(),
                address.getComplement(),
                address.getZipCode(),
                address.getCity(),
                address.getState()
        );
    }
}
