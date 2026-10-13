package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.dto.auth.LoginRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.auth.LoginResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.UnauthorizedActionException;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.domain.service.PasswordHasher;
import br.com.anhembi.supplychainverde.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthenticateUserUseCase {
    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;
    private final PasswordHasher passwordHasher;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponseDTO execute(LoginRequestDTO request) {
        if (request == null || request.email() == null || request.password() == null) {
            throw new UnauthorizedActionException("Credenciais inválidas.");
        }
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedActionException("Credenciais inválidas."));
        if (!passwordHasher.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedActionException("Credenciais inválidas.");
        }

        Long supplierId = null;
        if (user.getRole() == UserRole.SUPPLIER) {
            supplierId = supplierRepository.findByUserId(user.getUserId())
                    .map(Supplier::getSupplierId)
                    .orElseThrow(() -> new UnauthorizedActionException("Usuário fornecedor sem fornecedor vinculado."));
        }

        String token = jwtTokenProvider.generateToken(user, supplierId);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        return new LoginResponseDTO(token, expiresAt, user.getRole());
    }
}
