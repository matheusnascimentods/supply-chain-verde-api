package br.com.anhembi.supplychainverde.application.usecase.invite;

import br.com.anhembi.supplychainverde.application.dto.invite.SendInviteRequestDTO;
import br.com.anhembi.supplychainverde.application.exception.ConflictException;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.application.usecase.user.SeatPolicy;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.entity.Invite;
import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.repository.InviteRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.domain.service.EmailSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SendInviteUseCaseTest {
    private final InviteRepository invites = mock(InviteRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final EmailSender emailSender = mock(EmailSender.class);
    private final SendInviteUseCase useCase =
            new SendInviteUseCase(invites, users, companies, new SeatPolicy(users, invites), emailSender, "http://front.test");

    @Test
    void savesOnlyTheTokenHashAndMailsTheLinkWithTheToken() {
        givenCompany(CompanyType.MARKET, "Mercado <Bom>", 3);
        ArgumentCaptor<Invite> saved = ArgumentCaptor.forClass(Invite.class);
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);

        useCase.execute(new SendInviteRequestDTO(" ana@it.test ", UserRole.AUDITOR), 1L, 7L);

        verify(companies).lockById(7L);
        verify(invites).save(saved.capture());
        verify(emailSender).send(eq("ana@it.test"), anyString(), html.capture());
        Matcher link = Pattern.compile("http://front\\.test/convites/([A-Za-z0-9_-]{43})").matcher(html.getValue());
        assertThat(link.find()).isTrue();
        assertThat(saved.getValue().getTokenHash()).isEqualTo(InviteToken.hash(link.group(1)));
        assertThat(saved.getValue()).extracting(Invite::getInvitedBy, Invite::getInvitedFor, Invite::getRole)
                .containsExactly(1L, "ana@it.test", UserRole.AUDITOR);
        assertThat(html.getValue()).contains("Mercado &lt;Bom&gt;").contains("Auditor");
    }

    @Test
    void activeInvitesOccupySeats() {
        givenCompany(CompanyType.MARKET, "Mercado", 3);
        when(users.countByCompanyId(7L)).thenReturn(2L);
        when(invites.countActiveByCompanyId(7L)).thenReturn(1L);

        assertThatThrownBy(() -> useCase.execute(new SendInviteRequestDTO("ana@it.test", UserRole.MANAGER), 1L, 7L))
                .isInstanceOf(ConflictException.class).hasMessageContaining("Sem assentos livres: 3 de 3");
        verifyNoInteractions(emailSender);
        verify(invites, never()).save(any());
    }

    @Test
    void rejectsAnEmailWithAnActiveInviteOrAnExistingUser() {
        givenCompany(CompanyType.MARKET, "Mercado", 3);
        when(invites.existsActiveByEmail("ana@it.test")).thenReturn(true);
        when(users.existsByEmail("bia@it.test")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new SendInviteRequestDTO("ana@it.test", UserRole.MANAGER), 1L, 7L))
                .isInstanceOf(ConflictException.class).hasMessageContaining("convite ativo");
        assertThatThrownBy(() -> useCase.execute(new SendInviteRequestDTO("bia@it.test", UserRole.MANAGER), 1L, 7L))
                .isInstanceOf(ConflictException.class).hasMessageContaining("já cadastrado");
        verifyNoInteractions(emailSender);
    }

    @Test
    void rejectsRolesTheCompanyTypeDoesNotAllowAndInvalidEmails() {
        givenCompany(CompanyType.SUPPLIER, "Fornecedor", 3);

        assertThatThrownBy(() -> useCase.execute(new SendInviteRequestDTO("ana@it.test", UserRole.AUDITOR), 1L, 7L))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> useCase.execute(new SendInviteRequestDTO("sem-arroba", UserRole.SUPPLIER), 1L, 7L))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> useCase.execute(new SendInviteRequestDTO("ana@it.test", null), 1L, 7L))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void platformAdminHasNoCompanyToInviteTo() {
        assertThatThrownBy(() -> useCase.execute(new SendInviteRequestDTO("ana@it.test", UserRole.ADMIN), 1L, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    private void givenCompany(CompanyType type, String name, int maxUsers) {
        when(companies.findById(7L)).thenReturn(Optional.of(Company.builder().companyId(7L).name(name).type(type).maxUsers(maxUsers).build()));
    }
}
