package br.com.anhembi.supplychainverde.application.dto.chain;
import java.time.LocalDateTime;
import br.com.anhembi.supplychainverde.application.dto.address.AddressResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.transport.TransportResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.carbonemission.CarbonEmissionResponseDTO;
import br.com.anhembi.supplychainverde.domain.enums.StageType;
public record ChainResponseDTO(Long chainId, Long batchId, AddressResponseDTO originAddress, AddressResponseDTO destinationAddress, Long responsibleUserId, String responsibleUserName, StageType stageType, LocalDateTime startedAt, LocalDateTime endedAt, TransportResponseDTO transport, CarbonEmissionResponseDTO emission) {}
