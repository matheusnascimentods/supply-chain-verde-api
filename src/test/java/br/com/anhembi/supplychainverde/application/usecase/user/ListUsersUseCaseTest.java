package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.dto.user.UserResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListUsersUseCaseTest {
    private final UserRepository users = mock(UserRepository.class);
    private final SupplierRepository suppliers = mock(SupplierRepository.class);
    private final ListUsersUseCase useCase = new ListUsersUseCase(users, suppliers);

    @Test
    void shouldIncludeLinkedSupplierOnlyForSupplierUsers() {
        when(users.findPage(null, null, 20, 0)).thenReturn(List.of(
                User.builder().userId(1L).role(UserRole.ADMIN).build(),
                User.builder().userId(3L).role(UserRole.SUPPLIER).build()));
        when(users.countPage(null, null)).thenReturn(2L);
        when(suppliers.findByUserIdIn(any())).thenReturn(List.of(
                Supplier.builder().supplierId(7L).name("Fazenda Verde").userId(3L).build()));

        List<UserResponseDTO> items = useCase.execute(null, null, 20, 0).items();

        assertThat(items.get(0).supplierId()).isNull();
        assertThat(items.get(1).supplierId()).isEqualTo(7L);
        assertThat(items.get(1).supplierName()).isEqualTo("Fazenda Verde");
    }
}
