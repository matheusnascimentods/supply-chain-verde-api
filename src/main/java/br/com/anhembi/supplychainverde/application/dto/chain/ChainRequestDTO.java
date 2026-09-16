package br.com.anhembi.supplychainverde.application.dto.chain;
import java.time.LocalDateTime;
import br.com.anhembi.supplychainverde.domain.enums.StageType;
public record ChainRequestDTO(Long batchId, Long originAddressId, Long destinationAddressId, StageType stageType, LocalDateTime startedAt, LocalDateTime endedAt) {}
