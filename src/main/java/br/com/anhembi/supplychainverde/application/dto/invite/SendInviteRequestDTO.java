package br.com.anhembi.supplychainverde.application.dto.invite;

import br.com.anhembi.supplychainverde.domain.enums.UserRole;

public record SendInviteRequestDTO(String email, UserRole role) {}
