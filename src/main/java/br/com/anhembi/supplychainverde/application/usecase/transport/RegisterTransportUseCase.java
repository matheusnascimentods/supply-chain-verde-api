package br.com.anhembi.supplychainverde.application.usecase.transport;

import br.com.anhembi.supplychainverde.application.dto.transport.TransportRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.transport.TransportResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.entity.Chain;
import br.com.anhembi.supplychainverde.domain.entity.Transport;
import br.com.anhembi.supplychainverde.domain.repository.ChainRepository;
import br.com.anhembi.supplychainverde.domain.repository.TransportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterTransportUseCase {
    private final TransportRepository transportRepository;
    private final ChainRepository chainRepository;

    public TransportResponseDTO execute(TransportRequestDTO request) {
        if (request == null) throw new ValidationException("Requisição de transporte é obrigatória.");
        Chain chain = chainRepository.findById(request.chainId())
                .orElseThrow(() -> new IllegalArgumentException("Etapa não encontrada: " + request.chainId()));

        Transport transport = Transport.builder()
                .chain(chain)
                .transportMode(request.transportMode())
                .distance(request.distance())
                .fuelType(request.fuelType())
                .capacity(request.capacity())
                .build();
        Transport saved = transportRepository.save(transport);
        return new TransportResponseDTO(saved.getTransportId(), saved.getChain().getChainId(), saved.getTransportMode(), saved.getDistance(), saved.getFuelType(), saved.getCapacity());
    }
}
