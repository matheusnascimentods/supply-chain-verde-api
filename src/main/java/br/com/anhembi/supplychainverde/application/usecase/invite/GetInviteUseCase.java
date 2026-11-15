package br.com.anhembi.supplychainverde.application.usecase.invite;

import br.com.anhembi.supplychainverde.application.dto.invite.InviteDetailsResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.ResourceNotFoundException;
import br.com.anhembi.supplychainverde.domain.entity.Invite;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.repository.InviteRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RN-08 e RN-09: só lê. Token desconhecido, expirado ou aceito dá o mesmo 404. */
@Service
@RequiredArgsConstructor
public class GetInviteUseCase {
    static final String INVALID = "Convite inválido ou expirado.";

    private final InviteRepository inviteRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public InviteDetailsResponseDTO execute(String token) {
        Invite invite = findActive(inviteRepository, token);
        String companyName = userRepository.findById(invite.getInvitedBy())
                .flatMap(user -> companyRepository.findById(user.getCompanyId()))
                .orElseThrow(() -> new ResourceNotFoundException(INVALID))
                .getName();
        return new InviteDetailsResponseDTO(invite.getInvitedFor(), invite.getRole(), companyName, invite.getExpiresAt());
    }

    static Invite findActive(InviteRepository inviteRepository, String token) {
        if (token == null || token.isBlank()) throw new ResourceNotFoundException(INVALID);
        return inviteRepository.findActiveByTokenHash(InviteToken.hash(token))
                .orElseThrow(() -> new ResourceNotFoundException(INVALID));
    }
}
