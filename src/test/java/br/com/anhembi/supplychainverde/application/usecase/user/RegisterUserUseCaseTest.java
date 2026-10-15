package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.mapper.DtoMappers;
import br.com.anhembi.supplychainverde.application.dto.user.UserRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.user.UserResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.ConflictException;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.domain.service.PasswordHasher;
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

class RegisterUserUseCaseTest {
    private final UserRepository users = mock(UserRepository.class);
    private final SupplierRepository suppliers = mock(SupplierRepository.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final RegisterUserUseCase useCase = new RegisterUserUseCase(users, suppliers, hasher, DtoMappers.USER);

    @Test
    void shouldLinkSupplierUserToSupplierWithoutUser() {
        when(suppliers.findById(7L)).thenReturn(Optional.of(Supplier.builder().supplierId(7L).name("Fazenda Verde").build()));
        when(users.save(any())).thenReturn(User.builder().userId(3L).role(UserRole.SUPPLIER).build());

        UserResponseDTO response = useCase.execute(request(UserRole.SUPPLIER, 7L));

        assertThat(response.supplierId()).isEqualTo(7L);
        assertThat(response.supplierName()).isEqualTo("Fazenda Verde");
        verify(suppliers).save(argThat(supplier -> supplier.getUserId().equals(3L)));
    }

    @Test
    void shouldRequireSupplierIdForSupplierUser() {
        assertThatThrownBy(() -> useCase.execute(request(UserRole.SUPPLIER, null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectSupplierIdForNonSupplierUser() {
        assertThatThrownBy(() -> useCase.execute(request(UserRole.AUDITOR, 7L)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectDuplicatedEmail() {
        when(users.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(request(UserRole.AUDITOR, null)))
                .isInstanceOf(ConflictException.class);
        verify(users, never()).save(any());
    }

    @Test
    void shouldRejectSupplierThatAlreadyHasUser() {
        when(suppliers.findById(7L)).thenReturn(Optional.of(Supplier.builder().supplierId(7L).userId(9L).build()));

        assertThatThrownBy(() -> useCase.execute(request(UserRole.SUPPLIER, 7L)))
                .isInstanceOf(ConflictException.class);
        verify(users, never()).save(any());
    }

    private UserRequestDTO request(UserRole role, Long supplierId) {
        return new UserRequestDTO("Ana", "ana@example.com", "secret", role, supplierId);
    }
}
