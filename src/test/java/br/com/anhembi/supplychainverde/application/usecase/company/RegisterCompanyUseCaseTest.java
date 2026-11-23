package br.com.anhembi.supplychainverde.application.usecase.company;

import br.com.anhembi.supplychainverde.application.dto.address.AddressRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.auth.LoginResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.company.RegisterCompanyRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.company.RegisterCompanyRequestDTO.AdminData;
import br.com.anhembi.supplychainverde.application.dto.company.RegisterCompanyRequestDTO.CompanyData;
import br.com.anhembi.supplychainverde.application.exception.ConflictException;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.application.mapper.DtoMappers;
import br.com.anhembi.supplychainverde.application.usecase.user.AuthenticateUserUseCase;
import br.com.anhembi.supplychainverde.domain.entity.Address;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.repository.AddressRepository;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.domain.service.PasswordHasher;
import br.com.anhembi.supplychainverde.domain.valueobject.Cnpj;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class RegisterCompanyUseCaseTest {
    private static final String CNPJ = "11222333000181";

    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final AddressRepository addresses = mock(AddressRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final AuthenticateUserUseCase authenticate = mock(AuthenticateUserUseCase.class);
    private final RegisterCompanyUseCase useCase = new RegisterCompanyUseCase(
            companies, addresses, users, hasher, DtoMappers.ADDRESS, authenticate);

    @Test
    void createsCompanyAndAdminAndReturnsTheLoginToken() {
        when(addresses.save(any())).thenReturn(Address.builder().addressId(5L).build());
        when(companies.save(any())).thenAnswer(call -> {
            Company company = call.getArgument(0);
            company.setCompanyId(40L);
            return company;
        });
        when(hasher.hash("segredo123")).thenReturn("hash");
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));
        LoginResponseDTO token = new LoginResponseDTO("jwt", LocalDateTime.now(), UserRole.ADMIN, null, 40L, CompanyType.MARKET, "Mercado");
        when(authenticate.issueToken(any())).thenReturn(token);

        assertThat(useCase.execute(request(CNPJ, "ana@bompreco.com"))).isSameAs(token);

        verify(companies).save(argThat(company -> company.getType() == CompanyType.MARKET
                && company.getCnpj().value().equals(CNPJ) && company.getAddress().getAddressId() == 5L));
        verify(users).save(argThat((User user) -> user.getRole() == UserRole.ADMIN && user.getCompanyId() == 40L
                && user.getPassword().equals("hash")));
    }

    @Test
    void rejectsDuplicatedCnpjWithConflict() {
        when(companies.existsByCnpj(new Cnpj(CNPJ))).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(request(CNPJ, "ana@bompreco.com")))
                .isInstanceOf(ConflictException.class);
        verify(companies, never()).save(any());
    }

    @Test
    void rejectsDuplicatedAdminEmailWithConflict() {
        when(users.existsByEmail("ana@bompreco.com")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(request(CNPJ, "ana@bompreco.com")))
                .isInstanceOf(ConflictException.class);
        verify(companies, never()).save(any());
    }

    @Test
    void rejectsInvalidCnpjAndInvalidEmailAsBadRequest() {
        assertThatThrownBy(() -> useCase.execute(request("11222333000100", "ana@bompreco.com")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute(request(CNPJ, "ana-sem-arroba")))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(addresses);
    }

    private RegisterCompanyRequestDTO request(String cnpj, String adminEmail) {
        return new RegisterCompanyRequestDTO(CompanyType.MARKET,
                new CompanyData("Mercado Bom Preço LTDA", cnpj, "contato@bompreco.com", "11999990000",
                        new AddressRequestDTO("Rua A", "10", "Centro", null, "01001000", "São Paulo", "SP")),
                new AdminData("Ana Souza", adminEmail, "segredo123"));
    }
}
