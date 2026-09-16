package br.com.anhembi.supplychainverde.infrastructure.security;

import br.com.anhembi.supplychainverde.domain.service.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordHasher implements PasswordHasher {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Senha não pode ser nula ou vazia.");
        }
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hashedPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return false;
        }
        return encoder.matches(rawPassword, hashedPassword);
    }
}
