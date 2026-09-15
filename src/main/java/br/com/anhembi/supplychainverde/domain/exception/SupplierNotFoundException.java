package br.com.anhembi.supplychainverde.domain.exception;

public class SupplierNotFoundException extends DomainException {
    public SupplierNotFoundException(Long supplierId) {
        super("Fornecedor não encontrado: " + supplierId);
    }
}
