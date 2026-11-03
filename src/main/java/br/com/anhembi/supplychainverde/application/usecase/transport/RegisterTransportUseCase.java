package br.com.anhembi.supplychainverde.application.usecase.transport;

import org.springframework.security.access.AccessDeniedException;
import br.com.anhembi.supplychainverde.application.dto.transport.TransportRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.transport.TransportResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.entity.Chain;
import br.com.anhembi.supplychainverde.domain.entity.Transport;
import br.com.anhembi.supplychainverde.domain.repository.ChainRepository;
import br.com.anhembi.supplychainverde.domain.repository.TransportRepository;
import br.com.anhembi.supplychainverde.application.mapper.TransportDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterTransportUseCase {
    private final TransportRepository transportRepository;
    private final ChainRepository chainRepository;
    private final TransportDtoMapper mapper;

    @Transactional
    /** RN-15: só o mercado ou o fornecedor do lote da etapa; os dois nulos são o PLATFORM_ADMIN. */
    public TransportResponseDTO execute(TransportRequestDTO request, Long marketId, Long supplierId) {
        if (request == null) throw new ValidationException("Requisição de transporte é obrigatória.");
        Chain chain = chainRepository.findById(request.chainId())
                .orElseThrow(() -> new IllegalArgumentException("Etapa não encontrada: " + request.chainId()));
        if (!chain.getBatch().isWithin(marketId, supplierId)) throw new AccessDeniedException("Lote de outra companhia.");

        Transport transport = mapper.toDomain(request);
        transport.setChain(chain);
        return mapper.toResponse(transportRepository.save(transport));
    }
}
