package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.dto.user.UserResponseDTO;
import br.com.anhembi.supplychainverde.application.mapper.DtoMappers;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListUsersUseCaseTest {
    private final UserRepository users = mock(UserRepository.class);
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final ListUsersUseCase useCase = new ListUsersUseCase(users, companies, DtoMappers.USER);

    @Test
    void bringsTheCompanyAndRepeatsItAsSupplierOnlyForSupplierCompanies() {
        when(users.findPage(null, null, 20, 0)).thenReturn(List.of(
                User.builder().userId(1L).role(UserRole.PLATFORM_ADMIN).build(),
                User.builder().userId(2L).role(UserRole.MANAGER).companyId(40L).build(),
                User.builder().userId(3L).role(UserRole.ADMIN).companyId(7L).build()));
        when(users.countPage(null, null)).thenReturn(3L);
        when(companies.findAllById(any())).thenReturn(List.of(
                Company.builder().companyId(40L).name("Mercado").type(CompanyType.MARKET).build(),
                Company.builder().companyId(7L).name("Fazenda Verde").type(CompanyType.SUPPLIER).build()));

        List<UserResponseDTO> items = useCase.execute(null, null, 20, 0).items();

        assertThat(items.get(0).companyId()).isNull();
        assertThat(items.get(1).companyName()).isEqualTo("Mercado");
        assertThat(items.get(1).supplierId()).isNull();
        assertThat(items.get(2).supplierId()).isEqualTo(7L);
        assertThat(items.get(2).supplierName()).isEqualTo("Fazenda Verde");
        assertThat(items.get(2).companyType()).isEqualTo(CompanyType.SUPPLIER);
    }
}
