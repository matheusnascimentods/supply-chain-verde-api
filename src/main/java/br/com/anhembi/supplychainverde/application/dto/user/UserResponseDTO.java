package br.com.anhembi.supplychainverde.application.dto.user;
import java.time.LocalDateTime;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
public record UserResponseDTO(Long userId, String name, String email, UserRole role, LocalDateTime createdAt) {}
