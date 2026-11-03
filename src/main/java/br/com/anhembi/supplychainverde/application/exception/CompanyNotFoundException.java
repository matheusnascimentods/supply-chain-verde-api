package br.com.anhembi.supplychainverde.application.exception;

public class CompanyNotFoundException extends ResourceNotFoundException {
    public CompanyNotFoundException(Long companyId) {
        super("Companhia não encontrada: " + companyId);
    }
}
