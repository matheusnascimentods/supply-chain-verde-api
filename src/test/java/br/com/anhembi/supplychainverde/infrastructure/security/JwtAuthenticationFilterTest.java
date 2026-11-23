package br.com.anhembi.supplychainverde.infrastructure.security;

import org.junit.jupiter.api.AfterEach;
import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {
    private final JwtTokenProvider jwt = mock(JwtTokenProvider.class);
    private final UserRepository users = mock(UserRepository.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt, users);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateSupplierTokenWithSupplierId() throws Exception {
        givenToken("SUPPLIER", 7L, CompanyType.SUPPLIER);

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), new MockFilterChain());

        CustomUserPrincipal principal = (CustomUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.supplierId()).isEqualTo(7L);
        assertThat(principal.marketId()).isNull();
    }

    // Sem companhia as listagens não filtram: um token antigo, sem companyId, não pode virar visão global.
    @Test
    void shouldNotAuthenticateCompanyRoleWithoutCompany() throws Exception {
        givenToken("ADMIN", null, null);

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldAuthenticatePlatformAdminWithoutCompany() throws Exception {
        givenToken("PLATFORM_ADMIN", null, null);

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    // RN-11 (016): token válido de usuário desativado (ou apagado) não autentica.
    @Test
    void shouldNotAuthenticateAnInactiveUser() throws Exception {
        givenToken("ADMIN", 5L, CompanyType.MARKET);
        when(users.isActive(3L)).thenReturn(false);

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private void givenToken(String role, Long companyId, CompanyType companyType) {
        when(users.isActive(3L)).thenReturn(true);
        when(jwt.validateToken("token")).thenReturn(true);
        when(jwt.getUserIdFromToken("token")).thenReturn(3L);
        when(jwt.getEmailFromToken("token")).thenReturn("user@example.com");
        when(jwt.getRoleFromToken("token")).thenReturn(role);
        when(jwt.getCompanyIdFromToken("token")).thenReturn(companyId);
        when(jwt.getCompanyTypeFromToken("token")).thenReturn(companyType);
    }

    private MockHttpServletRequest requestWithToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        return request;
    }
}
