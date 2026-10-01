package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.dto.user.UserResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.pagination.OffsetPageResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListUsersUseCase {
    private final UserRepository userRepository;

    public OffsetPageResponseDTO<UserResponseDTO> execute(String email, int limit, int offset) {
        List<User> users = userRepository.findPage(email, limit, offset);
        long totalElements = userRepository.countPage(email);
        List<UserResponseDTO> items = users.stream()
                .map(user -> new UserResponseDTO(
                        user.getUserId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt()))
                .toList();
        return OffsetPageResponseDTO.of(items, limit, offset, totalElements);
    }
}
