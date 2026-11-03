package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.dto.user.UserRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.user.UserResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.CompanyNotFoundException;
import br.com.anhembi.supplychainverde.application.exception.ConflictException;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.application.mapper.UserDtoMapper;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.repository.PlanRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.domain.service.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PlanRepository planRepository;
    private final PasswordHasher passwordHasher;
    private final UserDtoMapper mapper;

    /** RN-09: o usuário nasce na companhia de quem cria. companyId nulo é o PLATFORM_ADMIN, que não cria usuários. */
    @Transactional
    public UserResponseDTO execute(UserRequestDTO request, Long companyId) {
        if (companyId == null) throw new AccessDeniedException("Só o ADMIN de uma companhia cria usuários.");
        if (request == null) throw new ValidationException("Dados do usuário são obrigatórios.");
        if (isBlank(request.name()) || isBlank(request.email()) || isBlank(request.password())) {
            throw new ValidationException("Nome, email e senha do usuário são obrigatórios.");
        }
        if (request.role() == null) throw new ValidationException("Perfil do usuário é obrigatório.");
        Company company = companyRepository.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
        if (!request.role().allowedFor(company.getType())) {
            throw new ValidationException("Perfil " + request.role() + " não é permitido em companhia " + company.getType() + ".");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email já cadastrado: " + request.email());
        }
        // RN-04: o lock impede que dois cadastros simultâneos passem juntos pela contagem.
        companyRepository.lockById(companyId);
        int maxUsers = planRepository.findActiveByCompanyId(companyId)
                .orElseThrow(() -> new ConflictException("Companhia sem assinatura ativa."))
                .maxUsers();
        if (userRepository.countByCompanyId(companyId) >= maxUsers) {
            throw new ConflictException("Limite de " + maxUsers + " usuários do plano atingido.");
        }

        User saved = userRepository.save(User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordHasher.hash(request.password()))
                .role(request.role())
                .companyId(companyId)
                .createdAt(LocalDateTime.now())
                .build());
        return mapper.toResponse(saved, company);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
