package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.dto.auth.LoginRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.auth.LoginResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.UnauthorizedActionException;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthenticateUserUseCase {
    private final UserRepository userRepository;

    public LoginResponseDTO execute(LoginRequestDTO request) {
        if (request == null || request.email() == null || request.password() == null) {
            throw new UnauthorizedActionException("Credenciais inválidas.");
        }
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedActionException("Credenciais inválidas."));
        if (!request.password().equals(user.getPassword())) {
            throw new UnauthorizedActionException("Credenciais inválidas.");
        }
        return new LoginResponseDTO("jwt-token-placeholder", LocalDateTime.now().plusHours(1), user.getRole());
    }
}
