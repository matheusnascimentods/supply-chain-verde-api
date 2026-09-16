package br.com.anhembi.supplychainverde.application.exception;

public class UnauthorizedActionException extends ApplicationException {
    public UnauthorizedActionException(String message) {
        super(message);
    }
}
