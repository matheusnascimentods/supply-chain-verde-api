package br.com.anhembi.supplychainverde.domain.exception;

import br.com.anhembi.supplychainverde.domain.enums.BatchStatus;

/** Transição de status fora de PENDING (RN-18, RN-19); vira 409. */
public class BatchStatusConflictException extends DomainException {
    public BatchStatusConflictException(Long batchId, BatchStatus status) {
        super("O lote " + batchId + " está " + status + " e não aceita esta ação.");
    }
}
