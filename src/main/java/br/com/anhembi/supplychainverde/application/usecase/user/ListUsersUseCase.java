package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.dto.user.UserPageDTO;
import br.com.anhembi.supplychainverde.application.dto.user.UserResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListUsersUseCase {
    private final UserRepository userRepository;

    public UserPageDTO execute(String email, int limit, int offset) {
        List<User> users = userRepository.findPage(email, limit + 1, offset);
        boolean hasNext = users.size() > limit;
        List<UserResponseDTO> items = users.stream()
                .limit(limit)
                .map(user -> new UserResponseDTO(
                        user.getUserId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt()))
                .toList();
        return new UserPageDTO(items, limit, offset, hasNext);
    }
}
