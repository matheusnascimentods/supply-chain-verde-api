package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.dto.user.UpdateUserRoleRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.user.UserResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateUserRoleUseCase {
    private final UserRepository userRepository;

    @Transactional
    public UserResponseDTO execute(Long userId, UpdateUserRoleRequestDTO request) {
        if (request == null || request.role() == null) throw new ValidationException("Perfil do usuário é obrigatório.");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + userId));
        user.updateRole(request.role());
        User updated = userRepository.save(user);
        return new UserResponseDTO(updated.getUserId(), updated.getName(), updated.getEmail(), updated.getRole(), updated.getCreatedAt());
    }
}
