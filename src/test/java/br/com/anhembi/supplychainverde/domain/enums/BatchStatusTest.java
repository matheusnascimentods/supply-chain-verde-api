package br.com.anhembi.supplychainverde.domain.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BatchStatusTest {
    @Test
    void onlyDeclaredLifecycleTransitionsAreValid() {
        assertThat(BatchStatus.PENDING.canTransitionTo(BatchStatus.ACTIVE)).isTrue();
        assertThat(BatchStatus.PENDING.canTransitionTo(BatchStatus.REJECTED)).isTrue();
        assertThat(BatchStatus.PENDING.canTransitionTo(BatchStatus.CANCELED)).isTrue();
        assertThat(BatchStatus.ACTIVE.canTransitionTo(BatchStatus.COMPLETED)).isTrue();

        for (BatchStatus from : BatchStatus.values()) {
            for (BatchStatus to : BatchStatus.values()) {
                boolean expected = (from == BatchStatus.PENDING
                        && (to == BatchStatus.ACTIVE || to == BatchStatus.REJECTED || to == BatchStatus.CANCELED))
                        || (from == BatchStatus.ACTIVE && to == BatchStatus.COMPLETED);
                assertThat(from.canTransitionTo(to)).isEqualTo(expected);
            }
        }
    }

    @Test
    void onlyPendingAndActiveAcceptStages() {
        assertThat(BatchStatus.PENDING.acceptsStages()).isTrue();
        assertThat(BatchStatus.ACTIVE.acceptsStages()).isTrue();
        assertThat(BatchStatus.COMPLETED.acceptsStages()).isFalse();
        assertThat(BatchStatus.REJECTED.acceptsStages()).isFalse();
        assertThat(BatchStatus.CANCELED.acceptsStages()).isFalse();
    }
}
