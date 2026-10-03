package br.com.anhembi.supplychainverde.application.dto.batch;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import br.com.anhembi.supplychainverde.application.dto.chain.ChainResponseDTO;
import br.com.anhembi.supplychainverde.domain.enums.StageType;
public record BatchResponseDTO(Long batchId, Long productId, String productName, Long supplierId, String supplierName,
        BigDecimal quantity, LocalDate producedAt, StageType currentStage, List<ChainResponseDTO> stages) {
    public BatchResponseDTO {
        stages = stages == null ? List.of() : List.copyOf(stages);
    }
}
