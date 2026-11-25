package br.com.anhembi.supplychainverde.application.usecase.chain;

import org.springframework.security.access.AccessDeniedException;
import br.com.anhembi.supplychainverde.domain.exception.BatchStatusConflictException;
import br.com.anhembi.supplychainverde.domain.enums.BatchStatus;
import br.com.anhembi.supplychainverde.application.dto.address.AddressRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.chain.ChainRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.chain.ChainResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.entity.Address;
import br.com.anhembi.supplychainverde.domain.entity.Batch;
import br.com.anhembi.supplychainverde.domain.entity.Chain;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.StageType;
import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import br.com.anhembi.supplychainverde.domain.exception.InvalidStageTransitionException;
import br.com.anhembi.supplychainverde.domain.repository.AddressRepository;
import br.com.anhembi.supplychainverde.domain.repository.BatchRepository;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.application.mapper.AddressDtoMapper;
import br.com.anhembi.supplychainverde.domain.repository.ChainRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.application.mapper.ChainDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RegisterChainStageUseCase {
    private final ChainRepository chainRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CompanyRepository companyRepository;
    private final AddressDtoMapper addressMapper;
    private final ChainDtoMapper mapper;

    public ChainResponseDTO execute(Long batchId, Long responsibleUserId, ChainRequestDTO request,
                                    Long marketId, Long supplierId) {
        return execute(batchId, responsibleUserId, request, marketId, supplierId, null);
    }

    @Transactional
    /** marketId e supplierId são o escopo de quem escreve; os dois nulos são o PLATFORM_ADMIN. */
    public ChainResponseDTO execute(Long batchId, Long responsibleUserId, ChainRequestDTO request,
                                    Long marketId, Long supplierId, CompanyType actorSide) {
        if (request == null) throw new ValidationException("Requisição de etapa é obrigatória.");
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Lote não encontrado: " + batchId));
        if (!batch.isWithin(marketId, supplierId)) throw new AccessDeniedException("Lote de outra companhia.");
        if (!batch.getStatus().acceptsStages()) throw new BatchStatusConflictException(batchId, batch.getStatus());
        if (request.stageType() == null) throw new ValidationException("Tipo da etapa é obrigatório.");
        Chain last = chainRepository.findByBatchId(batchId).stream()
                .max(Comparator.comparing(Chain::getStageType))
                .orElse(null);
        StageType current = last == null ? null : last.getStageType();
        StageType expected = current == null ? StageType.PRODUCTION : current.next();
        if (request.stageType() != expected) {
            throw new InvalidStageTransitionException(current, request.stageType());
        }
        if (actorSide != null && actorSide != expected.owner()) {
            String owner = expected.owner() == CompanyType.MARKET ? "mercado" : "fornecedor";
            throw new AccessDeniedException("A etapa " + expected + " é registrada pelo " + owner + " do lote.");
        }
        User responsibleUser = userRepository.findById(responsibleUserId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário responsável não encontrado: " + responsibleUserId));
        Long responsibleCompany = responsibleUser.getCompanyId();
        Long ownerCompany = expected.owner() == CompanyType.SUPPLIER
                ? batch.getSupplier().getCompanyId()
                : batch.getMarketId();
        if (!java.util.Objects.equals(responsibleCompany, ownerCompany)) {
            throw new AccessDeniedException("O responsável não é da companhia dona da etapa.");
        }
        Address destination = expected == StageType.RETAIL
                ? marketAddress(batch, request.destination())
                : addressRepository.save(validDestination(request.destination()));
        Address origin = last == null ? batch.supplierAddress() : last.getDestinationAddress(); // RN-04

        // A etapa começa agora e encerra a anterior: as datas são do servidor, não do cliente.
        LocalDateTime now = LocalDateTime.now();
        boolean pending = batch.getStatus() == BatchStatus.PENDING;
        if (pending) batch.activate(now.toLocalDate());
        if (last != null) chainRepository.closeOpenStages(batchId, now);
        if (expected == StageType.RETAIL) batch.complete();

        Chain stage = Chain.builder()
                .batch(batch)
                .destinationAddress(destination)
                .responsibleUser(responsibleUser)
                .stageType(request.stageType())
                .startedAt(now)
                .build();

        ChainResponseDTO saved = mapper.toResponse(chainRepository.save(stage), origin, null, null);
        if (pending || expected == StageType.RETAIL) batchRepository.save(batch);
        return saved;
    }

    /** RN-03 (018): o varejo termina no endereço atual do mercado do lote. */
    private Address marketAddress(Batch batch, AddressRequestDTO destination) {
        if (destination != null) throw new ValidationException("O destino do varejo é o endereço do mercado e não pode ser informado.");
        return companyRepository.findById(batch.getMarketId())
                .orElseThrow(() -> new IllegalArgumentException("Mercado do lote não encontrado: " + batch.getMarketId()))
                .getAddress();
    }

    /** RN-01 (018): endereço completo, só o complemento é opcional. */
    private Address validDestination(AddressRequestDTO destination) {
        if (destination == null) throw new ValidationException("Destino da etapa é obrigatório.");
        boolean incomplete = Stream.of(destination.street(), destination.number(), destination.neighborhood(),
                destination.zipCode(), destination.city(), destination.state()).anyMatch(v -> v == null || v.isBlank());
        if (incomplete) throw new ValidationException("Destino incompleto: informe logradouro, número, bairro, CEP, cidade e UF.");
        String complement = destination.complement() == null || destination.complement().isBlank() ? null : destination.complement().trim();
        return addressMapper.toDomain(new AddressRequestDTO(destination.street().trim(), destination.number().trim(),
                destination.neighborhood().trim(), complement, destination.zipCode().trim(), destination.city().trim(),
                destination.state().trim().toUpperCase()));
    }
}
