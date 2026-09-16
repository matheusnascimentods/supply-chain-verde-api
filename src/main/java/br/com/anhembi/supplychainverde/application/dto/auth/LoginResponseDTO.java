package br.com.anhembi.supplychainverde.application.dto.auth;
import java.time.LocalDateTime;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
public record LoginResponseDTO(String token, LocalDateTime expiresAt, UserRole role) {}
