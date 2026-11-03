package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.dto.user.UpdateUserRoleRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.user.UserResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.CompanyNotFoundException;
import br.com.anhembi.supplychainverde.application.exception.ConflictException;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.application.mapper.UserDtoMapper;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateUserRoleUseCase {
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final UserDtoMapper mapper;

    /** actorCompanyId nulo é o PLATFORM_ADMIN, que troca o perfil de qualquer companhia. */
    @Transactional
    public UserResponseDTO execute(Long userId, UpdateUserRoleRequestDTO request, Long actorCompanyId) {
        if (request == null || request.role() == null) throw new ValidationException("Perfil do usuário é obrigatório.");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + userId));
        Long companyId = user.getCompanyId();
        if (actorCompanyId != null && !actorCompanyId.equals(companyId)) {
            throw new AccessDeniedException("Usuário de outra companhia.");
        }
        if (companyId == null) throw new ValidationException("O perfil do PLATFORM_ADMIN não muda por endpoint.");
        Company company = companyRepository.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
        if (!request.role().allowedFor(company.getType())) {
            throw new ValidationException("Perfil " + request.role() + " não é permitido em companhia " + company.getType() + ".");
        }
        // RN-08: com o lock, duas trocas simultâneas não deixam a companhia sem ADMIN.
        companyRepository.lockById(companyId);
        if (user.getRole() == UserRole.ADMIN && request.role() != UserRole.ADMIN
                && userRepository.countByCompanyIdAndRole(companyId, UserRole.ADMIN) <= 1) {
            throw new ConflictException("A companhia precisa de pelo menos um ADMIN.");
        }
        user.updateRole(request.role());
        return mapper.toResponse(userRepository.save(user), company);
    }
}
