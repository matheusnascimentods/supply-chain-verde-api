package br.com.anhembi.supplychainverde.application.dto.user;

import java.util.List;

public record UserPageDTO(List<UserResponseDTO> items, int limit, int offset, boolean hasNext, int totalPages) {}
