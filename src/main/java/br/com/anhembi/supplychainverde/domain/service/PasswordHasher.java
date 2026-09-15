package br.com.anhembi.supplychainverde.domain.service;

public interface PasswordHasher {
    String hash(String rawPassword);

    boolean matches(String rawPassword, String hashedPassword);
}
