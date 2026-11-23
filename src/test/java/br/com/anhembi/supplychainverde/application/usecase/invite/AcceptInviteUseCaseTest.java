package br.com.anhembi.supplychainverde.application.usecase.invite;

import br.com.anhembi.supplychainverde.application.dto.auth.LoginResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.invite.AcceptInviteRequestDTO;
import br.com.anhembi.supplychainverde.application.exception.ConflictException;
import br.com.anhembi.supplychainverde.application.exception.ResourceNotFoundException;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.application.usecase.user.AuthenticateUserUseCase;
import br.com.anhembi.supplychainverde.domain.entity.Invite;
import br.com.anhembi.supplychainverde.application.usecase.user.SeatPolicy;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.repository.InviteRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.domain.service.PasswordHasher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AcceptInviteUseCaseTest {
    private static final String TOKEN = "token-do-link";

    private final InviteRepository invites = mock(InviteRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final AuthenticateUserUseCase authenticate = mock(AuthenticateUserUseCase.class);
    private final AcceptInviteUseCase useCase =
            new AcceptInviteUseCase(invites, users, companies, new SeatPolicy(users, invites), hasher, authenticate);

    private final Invite invite = Invite.builder()
            .inviteId(5L).invitedBy(1L).invitedFor("ana@it.test").role(UserRole.AUDITOR).tokenHash(InviteToken.hash(TOKEN)).build();

    @Test
    void createsTheUserFromTheInviteInTheInvitersCompanyAndMarksItAccepted() {
        // 2 usuários + este convite = 3 de 3: o convite já tinha reservado o assento.
        givenInviteFromCompany(7L, 3, 2);
        when(hasher.hash("segredo")).thenReturn("hash");
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));
        LoginResponseDTO login = mock(LoginResponseDTO.class);
        when(authenticate.issueToken(any())).thenReturn(login);
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);

        assertThat(useCase.execute(new AcceptInviteRequestDTO(TOKEN, " Ana ", "segredo"))).isSameAs(login);

        verify(companies).lockById(7L);
        verify(users).save(saved.capture());
        assertThat(saved.getValue()).extracting(User::getName, User::getEmail, User::getPassword, User::getRole, User::getCompanyId)
                .containsExactly("Ana", "ana@it.test", "hash", UserRole.AUDITOR, 7L);
        assertThat(invite.isAccepted()).isTrue();
        verify(invites).save(invite);
    }

    @Test
    void unknownExpiredOrAcceptedTokenIsNotFound() {
        when(invites.findActiveByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new AcceptInviteRequestDTO(TOKEN, "Ana", "segredo")))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Convite inválido ou expirado.");
        verifyNoInteractions(companies);
    }

    @Test
    void blankNameOrPasswordIsABadRequest() {
        assertThatThrownBy(() -> useCase.execute(new AcceptInviteRequestDTO(TOKEN, " ", "segredo")))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> useCase.execute(new AcceptInviteRequestDTO(TOKEN, "Ana", null)))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(invites);
    }

    @Test
    void aCompanyAboveItsSeatsAtAcceptanceIsAConflict() {
        givenInviteFromCompany(7L, 3, 3);

        assertThatThrownBy(() -> useCase.execute(new AcceptInviteRequestDTO(TOKEN, "Ana", "segredo")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("Sem assentos livres: 4 de 3");
        verify(users, never()).save(any());
        assertThat(invite.isAccepted()).isFalse();
    }

    @Test
    void anEmailRegisteredSinceTheInviteIsAConflict() {
        givenInviteFromCompany(7L, 3, 1);
        when(users.existsByEmail("ana@it.test")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new AcceptInviteRequestDTO(TOKEN, "Ana", "segredo")))
                .isInstanceOf(ConflictException.class);
        verify(users, never()).save(any());
    }

    private void givenInviteFromCompany(Long companyId, int maxUsers, long users) {
        when(invites.findActiveByTokenHash(InviteToken.hash(TOKEN))).thenReturn(Optional.of(invite));
        when(this.users.findById(1L)).thenReturn(Optional.of(User.builder().userId(1L).companyId(companyId).build()));
        when(companies.findById(companyId)).thenReturn(Optional.of(Company.builder().companyId(companyId).maxUsers(maxUsers).build()));
        when(this.users.countByCompanyId(companyId)).thenReturn(users);
        when(invites.countActiveByCompanyId(companyId)).thenReturn(1L);
    }
}
