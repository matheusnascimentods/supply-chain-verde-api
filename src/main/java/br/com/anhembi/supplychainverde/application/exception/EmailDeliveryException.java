package br.com.anhembi.supplychainverde.application.exception;

/** O provedor de email recusou ou não respondeu (RN-11 da feature 011): vira 502. */
public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
