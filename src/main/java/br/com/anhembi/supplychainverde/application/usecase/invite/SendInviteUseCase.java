package br.com.anhembi.supplychainverde.application.usecase.invite;

import br.com.anhembi.supplychainverde.application.dto.invite.SendInviteRequestDTO;
import br.com.anhembi.supplychainverde.application.exception.CompanyNotFoundException;
import br.com.anhembi.supplychainverde.application.exception.ConflictException;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.application.usecase.user.SeatPolicy;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.entity.Invite;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.repository.InviteRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.domain.service.EmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class SendInviteUseCase {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final InviteRepository inviteRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final SeatPolicy seatPolicy;
    private final EmailSender emailSender;
    private final String frontendUrl;

    public SendInviteUseCase(InviteRepository inviteRepository, UserRepository userRepository,
                             CompanyRepository companyRepository, SeatPolicy seatPolicy,
                             EmailSender emailSender, @Value("${app.frontend-url}") String frontendUrl) {
        this.inviteRepository = inviteRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.seatPolicy = seatPolicy;
        this.emailSender = emailSender;
        this.frontendUrl = frontendUrl;
    }

    /**
     * RN-01 a RN-05 e RN-11 da feature 011. O envio fica dentro da transação: se o SendGrid falha, o convite
     * não é gravado. ponytail: a companhia fica travada durante a chamada HTTP; mover para AFTER_COMMIT se pesar.
     */
    @Transactional
    public void execute(SendInviteRequestDTO request, Long adminUserId, Long companyId) {
        if (companyId == null) throw new AccessDeniedException("Só o ADMIN de uma companhia convida usuários.");
        if (request == null || request.email() == null || !EMAIL.matcher(request.email().trim()).matches()) {
            throw new ValidationException("Email do convidado é obrigatório e precisa ser válido.");
        }
        if (request.role() == null) throw new ValidationException("Perfil do convidado é obrigatório.");
        String email = request.email().trim();
        Company company = companyRepository.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
        if (!request.role().allowedFor(company.getType())) {
            throw new ValidationException("Perfil " + request.role() + " não é permitido em companhia " + company.getType() + ".");
        }
        if (userRepository.existsByEmail(email)) throw new ConflictException("Email já cadastrado: " + email);

        // RN-04 e RN-05: o lock serializa convites e aceites da companhia.
        companyRepository.lockById(companyId);
        if (inviteRepository.existsActiveByEmail(email)) {
            throw new ConflictException("Já existe um convite ativo para " + email + ".");
        }
        seatPolicy.requireFreeSeat(company);

        String token = InviteToken.generate();
        inviteRepository.save(Invite.builder()
                .invitedBy(adminUserId)
                .invitedFor(email)
                .role(request.role())
                .tokenHash(InviteToken.hash(token))
                .build());
        InviteEmail mail = InviteEmail.of(company.getName(), request.role(), frontendUrl + "/convites/" + token);
        emailSender.send(email, mail.subject(), mail.html());
    }
}
