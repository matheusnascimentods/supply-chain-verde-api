package br.com.anhembi.supplychainverde.domain.exception;

public class BatchNotFoundException extends DomainException {
    public BatchNotFoundException(Long batchId) {
        super("Lote não encontrado: " + batchId);
    }
}
