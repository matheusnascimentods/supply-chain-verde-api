package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.mapper.DtoMappers;
import br.com.anhembi.supplychainverde.application.dto.user.UpdateUserRoleRequestDTO;
import br.com.anhembi.supplychainverde.application.exception.ConflictException;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateUserRoleUseCaseTest {
    private final UserRepository users = mock(UserRepository.class);
    private final UpdateUserRoleUseCase useCase = new UpdateUserRoleUseCase(users, DtoMappers.USER);

    @Test
    void shouldRejectChangingRoleFromSupplier() {
        givenUserWithRole(UserRole.SUPPLIER);

        assertThatThrownBy(() -> useCase.execute(3L, new UpdateUserRoleRequestDTO(UserRole.AUDITOR)))
                .isInstanceOf(ConflictException.class);
        verify(users, never()).save(any());
    }

    @Test
    void shouldRejectChangingRoleToSupplier() {
        givenUserWithRole(UserRole.AUDITOR);

        assertThatThrownBy(() -> useCase.execute(3L, new UpdateUserRoleRequestDTO(UserRole.SUPPLIER)))
                .isInstanceOf(ConflictException.class);
        verify(users, never()).save(any());
    }

    private void givenUserWithRole(UserRole role) {
        when(users.findById(3L)).thenReturn(Optional.of(User.builder().userId(3L).role(role).build()));
    }
}
