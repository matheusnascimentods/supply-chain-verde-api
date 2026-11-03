package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.dto.user.UpdateUserRoleRequestDTO;
import br.com.anhembi.supplychainverde.application.exception.ConflictException;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.application.mapper.DtoMappers;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateUserRoleUseCaseTest {
    private final UserRepository users = mock(UserRepository.class);
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final UpdateUserRoleUseCase useCase = new UpdateUserRoleUseCase(users, companies, DtoMappers.USER);

    @BeforeEach
    void setUp() {
        when(companies.findById(5L)).thenReturn(Optional.of(Company.builder().companyId(5L).type(CompanyType.MARKET).build()));
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void rejectsDemotingTheLastAdmin() {
        givenUser(UserRole.ADMIN);
        when(users.countByCompanyIdAndRole(5L, UserRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> useCase.execute(3L, new UpdateUserRoleRequestDTO(UserRole.MANAGER), 5L))
                .isInstanceOf(ConflictException.class);
        verify(companies).lockById(5L);
        verify(users, never()).save(any());
    }

    @Test
    void demotesAnAdminWhenAnotherRemains() {
        givenUser(UserRole.ADMIN);
        when(users.countByCompanyIdAndRole(5L, UserRole.ADMIN)).thenReturn(2L);

        assertThat(useCase.execute(3L, new UpdateUserRoleRequestDTO(UserRole.MANAGER), 5L).role()).isEqualTo(UserRole.MANAGER);
    }

    @Test
    void rejectsUserOfAnotherCompanyUnlessPlatformAdmin() {
        givenUser(UserRole.AUDITOR);

        assertThatThrownBy(() -> useCase.execute(3L, new UpdateUserRoleRequestDTO(UserRole.MANAGER), 9L))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(useCase.execute(3L, new UpdateUserRoleRequestDTO(UserRole.MANAGER), null).role()).isEqualTo(UserRole.MANAGER);
    }

    @Test
    void rejectsPlatformAdminAndRolesOutsideTheCompanyType() {
        givenUser(UserRole.AUDITOR);

        assertThatThrownBy(() -> useCase.execute(3L, new UpdateUserRoleRequestDTO(UserRole.PLATFORM_ADMIN), 5L))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> useCase.execute(3L, new UpdateUserRoleRequestDTO(UserRole.SUPPLIER), 5L))
                .isInstanceOf(ValidationException.class);
        verify(users, never()).save(any());
    }

    private void givenUser(UserRole role) {
        when(users.findById(3L)).thenReturn(Optional.of(User.builder().userId(3L).role(role).companyId(5L).build()));
    }
}
