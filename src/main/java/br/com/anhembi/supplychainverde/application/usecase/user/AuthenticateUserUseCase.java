package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.dto.auth.LoginRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.auth.LoginResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.UnauthorizedActionException;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
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
    private final CompanyRepository companyRepository;
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
        return issueToken(user);
    }

    /** Mesma resposta para o login e para o cadastro de companhia (POST /companies). */
    public LoginResponseDTO issueToken(User user) {
        CompanyType companyType = user.getCompanyId() == null ? null : companyRepository.findById(user.getCompanyId())
                .map(Company::getType)
                .orElseThrow(() -> new UnauthorizedActionException("Usuário sem companhia vinculada."));
        // RF-04: supplierId continua no token, igual a companyId quando a companhia é fornecedora.
        Long supplierId = companyType == CompanyType.SUPPLIER ? user.getCompanyId() : null;
        if (user.getRole() == UserRole.SUPPLIER && supplierId == null) {
            throw new UnauthorizedActionException("Usuário fornecedor sem fornecedor vinculado.");
        }

        String token = jwtTokenProvider.generateToken(user, supplierId);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        return new LoginResponseDTO(token, expiresAt, user.getRole(), supplierId, user.getCompanyId(), companyType);
    }
}
