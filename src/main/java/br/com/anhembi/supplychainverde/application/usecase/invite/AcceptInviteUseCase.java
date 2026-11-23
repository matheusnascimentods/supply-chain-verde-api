package br.com.anhembi.supplychainverde.application.usecase.invite;

import br.com.anhembi.supplychainverde.application.dto.auth.LoginResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.invite.AcceptInviteRequestDTO;
import br.com.anhembi.supplychainverde.application.exception.CompanyNotFoundException;
import br.com.anhembi.supplychainverde.application.exception.ConflictException;
import br.com.anhembi.supplychainverde.application.exception.ResourceNotFoundException;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.application.usecase.user.AuthenticateUserUseCase;
import br.com.anhembi.supplychainverde.application.usecase.user.SeatPolicy;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.entity.Invite;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.repository.InviteRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.domain.service.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static br.com.anhembi.supplychainverde.application.usecase.invite.GetInviteUseCase.INVALID;
import static br.com.anhembi.supplychainverde.application.usecase.invite.GetInviteUseCase.findActive;

/**
 * RN-05, RN-07, RN-08 e RN-10: cria o usuário com email e perfil do convite, na companhia de quem convidou, e
 * marca o convite como aceito na mesma transação. A resposta é a mesma do login.
 */
@Service
@RequiredArgsConstructor
public class AcceptInviteUseCase {
    private final InviteRepository inviteRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final SeatPolicy seatPolicy;
    private final PasswordHasher passwordHasher;
    private final AuthenticateUserUseCase authenticate;

    @Transactional
    public LoginResponseDTO execute(AcceptInviteRequestDTO request) {
        if (request == null || isBlank(request.name()) || isBlank(request.password())) {
            throw new ValidationException("Nome e senha são obrigatórios.");
        }
        Invite invite = findActive(inviteRepository, request.token());
        Long companyId = userRepository.findById(invite.getInvitedBy())
                .orElseThrow(() -> new ResourceNotFoundException(INVALID))
                .getCompanyId();

        // Sob o lock, busca de novo: um aceite concorrente do mesmo token já terá marcado accepted.
        companyRepository.lockById(companyId);
        invite = findActive(inviteRepository, request.token());
        if (userRepository.existsByEmail(invite.getInvitedFor())) {
            throw new ConflictException("Email já cadastrado: " + invite.getInvitedFor());
        }
        // RN-02 (016): o convite aceito já ocupa o próprio assento, então só barra se a companhia passou do teto
        // depois do convite (o teto baixou por SQL, por exemplo).
        Company company = companyRepository.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
        long used = seatPolicy.seatsUsed(companyId);
        if (used > company.getMaxUsers()) throw SeatPolicy.full(used, company.getMaxUsers());

        User user = userRepository.save(User.builder()
                .name(request.name().trim())
                .email(invite.getInvitedFor())
                .password(passwordHasher.hash(request.password()))
                .role(invite.getRole())
                .companyId(companyId)
                .createdAt(LocalDateTime.now())
                .build());
        invite.accept();
        inviteRepository.save(invite);
        return authenticate.issueToken(user);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
