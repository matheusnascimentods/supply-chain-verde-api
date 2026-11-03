package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.TestPrincipals;
import br.com.anhembi.supplychainverde.application.dto.user.UpdateUserRoleRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.user.UserRequestDTO;
import br.com.anhembi.supplychainverde.application.mapper.DtoMappers;
import br.com.anhembi.supplychainverde.application.usecase.user.ListUsersUseCase;
import br.com.anhembi.supplychainverde.application.usecase.user.RegisterUserUseCase;
import br.com.anhembi.supplychainverde.application.usecase.user.UpdateUserRoleUseCase;
import br.com.anhembi.supplychainverde.application.usecase.user.UpdateOwnUserUseCase;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UserControllerTest {
    private final RegisterUserUseCase registerUser = mock(RegisterUserUseCase.class);
    private final ListUsersUseCase listUsers = mock(ListUsersUseCase.class);
    private final UpdateUserRoleUseCase updateRole = mock(UpdateUserRoleUseCase.class);
    private final UserController controller = new UserController(registerUser, updateRole, listUsers, mock(UpdateOwnUserUseCase.class),
            mock(UserRepository.class), mock(CompanyRepository.class), DtoMappers.USER);
    private final CustomUserPrincipal admin = TestPrincipals.market(1L, "ADMIN", 40L);

    @Test
    void shouldCreateUsersInTheCreatorCompany() {
        UserRequestDTO request = new UserRequestDTO("Ana", "ana@example.com", "secret", UserRole.AUDITOR);

        controller.create(request, admin);

        verify(registerUser).execute(request, 40L);
    }

    @Test
    void shouldListOnlyTheCompanyUsers() {
        controller.list(null, 20, 0, TestPrincipals.market(2L, "AUDITOR", 40L));

        verify(listUsers).execute(null, 40L, 20, 0);
    }

    @Test
    void shouldScopeRoleChangesToTheActorCompany() {
        UpdateUserRoleRequestDTO request = new UpdateUserRoleRequestDTO(UserRole.MANAGER);

        controller.updateRole(9L, request, admin);

        verify(updateRole).execute(9L, request, 40L);
    }
}
