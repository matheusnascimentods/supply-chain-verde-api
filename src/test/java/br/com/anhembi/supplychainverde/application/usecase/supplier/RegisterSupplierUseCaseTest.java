package br.com.anhembi.supplychainverde.application.usecase.supplier;

import br.com.anhembi.supplychainverde.application.dto.address.AddressRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.NewSupplierUserDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.user.UserRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.user.UserResponseDTO;
import br.com.anhembi.supplychainverde.application.usecase.user.RegisterUserUseCase;
import br.com.anhembi.supplychainverde.domain.entity.Address;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.repository.AddressRepository;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RegisterSupplierUseCaseTest {
    private final SupplierRepository suppliers = mock(SupplierRepository.class);
    private final AddressRepository addresses = mock(AddressRepository.class);
    private final RegisterUserUseCase registerUser = mock(RegisterUserUseCase.class);
    private final RegisterSupplierUseCase useCase = new RegisterSupplierUseCase(suppliers, addresses, registerUser);

    @Test
    void shouldCreateLinkedSupplierUserWhenUserBlockIsSent() {
        givenSavedSupplier();
        UserRequestDTO expectedUser = new UserRequestDTO("Ana", "ana@example.com", "secret", UserRole.SUPPLIER, 7L);
        when(registerUser.execute(expectedUser))
                .thenReturn(new UserResponseDTO(3L, "Ana", "ana@example.com", UserRole.SUPPLIER, null, 7L, "Verde"));

        var response = useCase.execute(request(new NewSupplierUserDTO("Ana", "ana@example.com", "secret")));

        verify(registerUser).execute(expectedUser);
        assertThat(response.userId()).isEqualTo(3L);
    }

    @Test
    void shouldCreateSupplierWithoutUserWhenUserBlockIsMissing() {
        givenSavedSupplier();

        assertThat(useCase.execute(request(null)).userId()).isNull();
        verifyNoInteractions(registerUser);
    }

    private void givenSavedSupplier() {
        when(addresses.save(any())).thenReturn(Address.builder().addressId(1L).build());
        when(suppliers.save(any())).thenReturn(Supplier.builder().supplierId(7L).name("Verde").build());
    }

    private SupplierRequestDTO request(NewSupplierUserDTO user) {
        return new SupplierRequestDTO("Verde", "11.222.333/0001-81",
                new AddressRequestDTO("Rua A", "1", "Centro", null, "01000-000", "São Paulo", "SP"), "11999999999", user);
    }
}
