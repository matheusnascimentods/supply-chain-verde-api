package br.com.anhembi.supplychainverde.application.dto.invite;

import br.com.anhembi.supplychainverde.domain.enums.UserRole;

import java.time.LocalDateTime;

public record InviteDetailsResponseDTO(String email, UserRole role, String companyName, LocalDateTime expiresAt) {}
