package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.dto.user.UpdateOwnUserRequestDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.domain.service.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateOwnUserUseCaseTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final UpdateOwnUserUseCase useCase = new UpdateOwnUserUseCase(users, new OwnAccountChanges(hasher));
    private final User user = User.builder().userId(3L).name("Ana").password("hash-atual").build();

    @BeforeEach
    void setUp() {
        when(users.findById(3L)).thenReturn(Optional.of(user));
        when(hasher.matches("senha-atual", "hash-atual")).thenReturn(true);
        when(hasher.hash("senha-nova")).thenReturn("hash-nova");
    }

    @Test
    void shouldChangeOnlyName() {
        useCase.execute(3L, new UpdateOwnUserRequestDTO("Ana S.", null, null));

        verify(users).save(argThat(saved -> saved.getName().equals("Ana S.") && saved.getPassword().equals("hash-atual")));
    }

    @Test
    void shouldChangePasswordWithCorrectCurrentPassword() {
        useCase.execute(3L, new UpdateOwnUserRequestDTO(null, "senha-atual", "senha-nova"));

        verify(users).save(argThat(saved -> saved.getName().equals("Ana") && saved.getPassword().equals("hash-nova")));
    }

    @Test
    void shouldNotSaveWhenCurrentPasswordIsWrongOrMissing() {
        assertThatThrownBy(() -> useCase.execute(3L, new UpdateOwnUserRequestDTO("Ana S.", "errada", "senha-nova")))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> useCase.execute(3L, new UpdateOwnUserRequestDTO(null, null, "senha-nova")))
                .isInstanceOf(ValidationException.class);

        assertThat(user.getName()).isEqualTo("Ana");
        verify(users, never()).save(any());
    }

    @Test
    void shouldRejectBlankNameOrPassword() {
        assertThatThrownBy(() -> useCase.execute(3L, new UpdateOwnUserRequestDTO(" ", null, null)))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> useCase.execute(3L, new UpdateOwnUserRequestDTO(null, "senha-atual", " ")))
                .isInstanceOf(ValidationException.class);
        verify(users, never()).save(any());
    }

    @Test
    void shouldNotSaveWhenNothingWasSent() {
        useCase.execute(3L, new UpdateOwnUserRequestDTO(null, null, null));

        verify(users, never()).save(any());
    }
}
