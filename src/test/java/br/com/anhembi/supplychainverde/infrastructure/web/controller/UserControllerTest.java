package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.user.UserRequestDTO;
import br.com.anhembi.supplychainverde.application.usecase.user.ListUsersUseCase;
import br.com.anhembi.supplychainverde.application.usecase.user.RegisterUserUseCase;
import br.com.anhembi.supplychainverde.application.usecase.user.UpdateUserRoleUseCase;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class UserControllerTest {
    private final RegisterUserUseCase registerUser = mock(RegisterUserUseCase.class);
    private final ListUsersUseCase listUsers = mock(ListUsersUseCase.class);
    private final UserController controller = new UserController(registerUser, mock(UpdateUserRoleUseCase.class), listUsers,
            mock(UserRepository.class), mock(SupplierRepository.class));
    private final CustomUserPrincipal manager = new CustomUserPrincipal(4L, "m@example.com", "MANAGER", null);
    private final CustomUserPrincipal admin = new CustomUserPrincipal(1L, "a@example.com", "ADMIN", null);

    @Test
    void shouldDenyManagerCreatingNonSupplierUser() {
        assertThatThrownBy(() -> controller.create(request(UserRole.ADMIN), manager))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(registerUser);
    }

    @Test
    void shouldAllowManagerCreatingSupplierUser() {
        UserRequestDTO request = request(UserRole.SUPPLIER);

        controller.create(request, manager);

        verify(registerUser).execute(request);
    }

    @Test
    void shouldListOnlySupplierUsersForManager() {
        controller.list(null, 20, 0, manager);

        verify(listUsers).execute(null, UserRole.SUPPLIER, 20, 0);
    }

    @Test
    void shouldListAllUsersForAdmin() {
        controller.list(null, 20, 0, admin);

        verify(listUsers).execute(null, null, 20, 0);
    }

    private UserRequestDTO request(UserRole role) {
        return new UserRequestDTO("Ana", "ana@example.com", "secret", role, role == UserRole.SUPPLIER ? 7L : null);
    }
}
