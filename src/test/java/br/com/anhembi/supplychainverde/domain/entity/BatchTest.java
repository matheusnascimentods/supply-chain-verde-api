package br.com.anhembi.supplychainverde.domain.entity;

import br.com.anhembi.supplychainverde.domain.enums.BatchStatus;
import br.com.anhembi.supplychainverde.domain.exception.BatchStatusConflictException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BatchTest {
    @Test
    void pendingBatchIsRejectedWithReasonAndRejectedIsFinal() {
        Batch batch = pending();

        batch.reject("  Sem capacidade  ");

        assertThat(batch.getStatus()).isEqualTo(BatchStatus.REJECTED);
        assertThat(batch.getRejectionReason()).isEqualTo("Sem capacidade");
        assertThatThrownBy(() -> batch.reject("de novo")).isInstanceOf(BatchStatusConflictException.class);
        assertThatThrownBy(() -> batch.activate(LocalDate.now())).isInstanceOf(BatchStatusConflictException.class);
    }

    @Test
    void rejectionNeedsAReason() {
        assertThatThrownBy(() -> pending().reject(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pendingBatchIsActivatedOnceWithTheProductionDate() {
        Batch batch = pending();

        batch.activate(LocalDate.of(2026, 3, 4));

        assertThat(batch.getStatus()).isEqualTo(BatchStatus.ACTIVE);
        assertThat(batch.getProducedAt()).isEqualTo(LocalDate.of(2026, 3, 4));
        assertThatThrownBy(() -> batch.activate(LocalDate.now())).isInstanceOf(BatchStatusConflictException.class);
        assertThatThrownBy(() -> batch.reject("tarde demais")).isInstanceOf(BatchStatusConflictException.class);
    }

    private static Batch pending() {
        return Batch.builder().batchId(1L).status(BatchStatus.PENDING).build();
    }
}
