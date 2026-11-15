package br.com.anhembi.supplychainverde.domain.service;

public interface EmailSender {
    /** Lança EmailDeliveryException se o email não for aceito pelo provedor. */
    void send(String to, String subject, String html);
}
