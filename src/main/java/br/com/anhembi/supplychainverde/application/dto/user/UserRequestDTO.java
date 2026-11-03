package br.com.anhembi.supplychainverde.application.dto.user;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
public record UserRequestDTO(String name, String email, String password, UserRole role) {}
