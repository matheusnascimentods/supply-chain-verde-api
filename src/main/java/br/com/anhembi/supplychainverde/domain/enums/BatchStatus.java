package br.com.anhembi.supplychainverde.domain.enums;

/** Ciclo de vida do lote: PENDING -> ACTIVE | REJECTED | CANCELED; ACTIVE -> COMPLETED. */
public enum BatchStatus {
    PENDING, ACTIVE, COMPLETED, REJECTED, CANCELED;

    public boolean canTransitionTo(BatchStatus next) {
        return switch (this) {
            case PENDING -> next == ACTIVE || next == REJECTED || next == CANCELED;
            case ACTIVE -> next == COMPLETED;
            case COMPLETED, REJECTED, CANCELED -> false;
        };
    }

    public boolean acceptsStages() {
        return this == PENDING || this == ACTIVE;
    }
}
