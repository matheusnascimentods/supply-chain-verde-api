package br.com.anhembi.supplychainverde.infrastructure.security;

public record CustomUserPrincipal(Long userId, String email, String role) {
}
