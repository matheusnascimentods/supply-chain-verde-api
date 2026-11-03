package br.com.anhembi.supplychainverde.domain.entity;

import br.com.anhembi.supplychainverde.domain.enums.BatchStatus;
import br.com.anhembi.supplychainverde.domain.exception.BatchStatusConflictException;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Batch {
    private Long batchId;
    private Product product;
    private Company supplier;
    private Long marketId;
    private String marketName;
    private BatchStatus status;
    private String rejectionReason;
    private BigDecimal quantity;
    private LocalDate producedAt;

    /** RN-19: só um lote PENDING é recusado, com motivo; REJECTED é final. */
    public void reject(String reason) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("O motivo da recusa é obrigatório.");
        if (status != BatchStatus.PENDING) throw new BatchStatusConflictException(batchId, status);
        status = BatchStatus.REJECTED;
        rejectionReason = reason.trim();
    }

    /** RN-18 e RN-20: a etapa PRODUCTION ativa o lote e define produced_at. */
    public void activate(LocalDate producedAt) {
        if (status != BatchStatus.PENDING) throw new BatchStatusConflictException(batchId, status);
        if (producedAt == null) throw new IllegalArgumentException("A data de produção é obrigatória.");
        status = BatchStatus.ACTIVE;
        this.producedAt = producedAt;
    }

    /** Escopo de leitura e escrita (RN-12, RN-15): nulos são o PLATFORM_ADMIN, que alcança qualquer lote. */
    public boolean isWithin(Long marketId, Long supplierId) {
        return (marketId == null || marketId.equals(this.marketId))
                && (supplierId == null || (supplier != null && supplierId.equals(supplier.getCompanyId())));
    }
}
