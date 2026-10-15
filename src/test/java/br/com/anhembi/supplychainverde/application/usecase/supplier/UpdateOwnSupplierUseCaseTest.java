package br.com.anhembi.supplychainverde.application.usecase.supplier;

import br.com.anhembi.supplychainverde.application.mapper.DtoMappers;
import br.com.anhembi.supplychainverde.application.dto.supplier.UpdateOwnSupplierRequestDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.repository.AddressRepository;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UpdateOwnSupplierUseCaseTest {
    private final SupplierRepository suppliers = mock(SupplierRepository.class);
    private final AddressRepository addresses = mock(AddressRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final UpdateOwnSupplierUseCase useCase =
            new UpdateOwnSupplierUseCase(suppliers, addresses, users, hasher, mock(GetSupplierUseCase.class), DtoMappers.ADDRESS);
    private final Supplier supplier = Supplier.builder().supplierId(7L).name("Antiga").phone("1100000000").userId(3L).build();
    private final User user = User.builder().userId(3L).name("Ana").password("hash-atual").build();

    @BeforeEach
    void setUp() {
        when(suppliers.findById(7L)).thenReturn(Optional.of(supplier));
        when(users.findById(3L)).thenReturn(Optional.of(user));
        when(hasher.matches("senha-atual", "hash-atual")).thenReturn(true);
        when(hasher.hash("senha-nova")).thenReturn("hash-nova");
    }

    @Test
    void shouldChangeOnlySentFields() {
        useCase.execute(7L, new UpdateOwnSupplierRequestDTO("Nova Razão", null, null, null, null, null));

        assertThat(supplier.getName()).isEqualTo("Nova Razão");
        assertThat(supplier.getPhone()).isEqualTo("1100000000");
        verify(suppliers).save(supplier);
        verify(users, never()).save(any());
        verifyNoInteractions(addresses);
    }

    @Test
    void shouldChangeUserNameAndPasswordWithCorrectCurrentPassword() {
        useCase.execute(7L, new UpdateOwnSupplierRequestDTO(null, null, null, "Ana S.", "senha-atual", "senha-nova"));

        verify(users).save(argThat(saved -> saved.getName().equals("Ana S.") && saved.getPassword().equals("hash-nova")));
    }

    @Test
    void shouldNotChangeAnythingWhenCurrentPasswordIsWrong() {
        assertThatThrownBy(() -> useCase.execute(7L,
                new UpdateOwnSupplierRequestDTO("Nova Razão", null, null, null, "errada", "senha-nova")))
                .isInstanceOf(ValidationException.class);

        assertThat(supplier.getName()).isEqualTo("Antiga");
        verify(suppliers, never()).save(any());
        verify(users, never()).save(any());
    }

    @Test
    void shouldRequireCurrentPasswordToChangePassword() {
        assertThatThrownBy(() -> useCase.execute(7L,
                new UpdateOwnSupplierRequestDTO(null, null, null, null, null, "senha-nova")))
                .isInstanceOf(ValidationException.class);
        verify(users, never()).save(any());
    }
}
