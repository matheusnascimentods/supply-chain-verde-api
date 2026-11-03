package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.dto.user.UserRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.user.UserResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.ConflictException;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.application.mapper.DtoMappers;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.entity.Plan;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.repository.PlanRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.domain.service.PasswordHasher;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class RegisterUserUseCaseTest {
    private final UserRepository users = mock(UserRepository.class);
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final PlanRepository plans = mock(PlanRepository.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final RegisterUserUseCase useCase = new RegisterUserUseCase(users, companies, plans, hasher, DtoMappers.USER);

    @Test
    void createsTheUserInTheCreatorCompany() {
        givenCompany(7L, CompanyType.SUPPLIER, 3);
        when(users.countByCompanyId(7L)).thenReturn(2L);
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));

        UserResponseDTO response = useCase.execute(request(UserRole.SUPPLIER), 7L);

        assertThat(response.companyId()).isEqualTo(7L);
        assertThat(response.supplierId()).isEqualTo(7L);
        verify(companies).lockById(7L);
        verify(users).save(argThat(user -> user.getCompanyId().equals(7L) && user.getRole() == UserRole.SUPPLIER));
    }

    @Test
    void rejectsTheUserBeyondThePlanLimit() {
        givenCompany(7L, CompanyType.MARKET, 3);
        when(users.countByCompanyId(7L)).thenReturn(3L);

        assertThatThrownBy(() -> useCase.execute(request(UserRole.AUDITOR), 7L)).isInstanceOf(ConflictException.class);
        verify(users, never()).save(any());
    }

    @Test
    void rejectsARoleThatTheCompanyTypeDoesNotAllow() {
        givenCompany(7L, CompanyType.MARKET, 3);

        assertThatThrownBy(() -> useCase.execute(request(UserRole.SUPPLIER), 7L)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> useCase.execute(request(UserRole.PLATFORM_ADMIN), 7L)).isInstanceOf(ValidationException.class);
        verify(users, never()).save(any());
    }

    @Test
    void rejectsDuplicatedEmail() {
        givenCompany(7L, CompanyType.MARKET, 3);
        when(users.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(request(UserRole.AUDITOR), 7L)).isInstanceOf(ConflictException.class);
        verify(users, never()).save(any());
    }

    @Test
    void rejectsCreatorWithoutCompany() {
        assertThatThrownBy(() -> useCase.execute(request(UserRole.AUDITOR), null)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(users);
    }

    private void givenCompany(Long id, CompanyType type, int maxUsers) {
        when(companies.findById(id)).thenReturn(Optional.of(Company.builder().companyId(id).type(type).name("Companhia").build()));
        when(plans.findActiveByCompanyId(id)).thenReturn(Optional.of(new Plan(1L, "BASIC", "Basic", maxUsers)));
    }

    private UserRequestDTO request(UserRole role) {
        return new UserRequestDTO("Ana", "ana@example.com", "secret", role);
    }
}
