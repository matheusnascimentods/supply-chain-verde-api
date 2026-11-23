package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.dto.user.UpdateUserStatusRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.user.UserResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.CompanyNotFoundException;
import br.com.anhembi.supplychainverde.application.exception.ConflictException;
import br.com.anhembi.supplychainverde.application.exception.ResourceNotFoundException;
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

/** RN-05 a RN-10 da feature 016: ativa ou desativa um usuário, com as mesmas permissões da troca de perfil. */
@Service
@RequiredArgsConstructor
public class SetUserStatusUseCase {
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final SeatPolicy seatPolicy;
    private final UserDtoMapper mapper;

    /** actorCompanyId nulo é o PLATFORM_ADMIN, que ativa e desativa em qualquer companhia. */
    @Transactional
    public UserResponseDTO execute(Long userId, UpdateUserStatusRequestDTO request, Long actorUserId, Long actorCompanyId) {
        if (request == null || request.active() == null) throw new ValidationException("Situação do usuário é obrigatória.");
        boolean active = request.active();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + userId));
        Long companyId = user.getCompanyId();
        if (actorCompanyId != null && !actorCompanyId.equals(companyId)) {
            throw new AccessDeniedException("Usuário de outra companhia.");
        }
        if (companyId == null) throw new ValidationException("O PLATFORM_ADMIN não é ativado nem desativado por endpoint.");
        Company company = companyRepository.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
        if (user.isActive() == active) return mapper.toResponse(user, company);
        if (!active && user.getUserId().equals(actorUserId)) throw new ConflictException("Você não pode desativar a si mesmo.");

        // RN-07 e RN-09: com o lock, duas mudanças simultâneas não deixam a companhia sem ADMIN nem furam o teto.
        companyRepository.lockById(companyId);
        if (active) {
            seatPolicy.requireFreeSeat(company);
        } else if (user.getRole() == UserRole.ADMIN && userRepository.countActiveByCompanyIdAndRole(companyId, UserRole.ADMIN) <= 1) {
            throw new ConflictException("A companhia precisa de pelo menos um ADMIN ativo.");
        }
        user.setActive(active);
        return mapper.toResponse(userRepository.save(user), company);
    }
}
