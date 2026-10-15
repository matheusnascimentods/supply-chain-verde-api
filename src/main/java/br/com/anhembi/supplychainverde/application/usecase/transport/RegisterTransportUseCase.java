package br.com.anhembi.supplychainverde.application.usecase.transport;

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
    public TransportResponseDTO execute(TransportRequestDTO request) {
        if (request == null) throw new ValidationException("Requisição de transporte é obrigatória.");
        Chain chain = chainRepository.findById(request.chainId())
                .orElseThrow(() -> new IllegalArgumentException("Etapa não encontrada: " + request.chainId()));

        Transport transport = mapper.toDomain(request);
        transport.setChain(chain);
        return mapper.toResponse(transportRepository.save(transport));
    }
}
