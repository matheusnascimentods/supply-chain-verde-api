package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.dto.user.UserRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.user.UserResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.domain.service.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public UserResponseDTO execute(UserRequestDTO request) {
        if (request == null) throw new ValidationException("Dados do usuário são obrigatórios.");
        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordHasher.hash(request.password()))
                .role(request.role())
                .createdAt(LocalDateTime.now())
                .build();
        User saved = userRepository.save(user);
        return new UserResponseDTO(saved.getUserId(), saved.getName(), saved.getEmail(), saved.getRole(), saved.getCreatedAt());
    }
}
