package br.com.anhembi.supplychainverde.infrastructure.security;

import org.junit.jupiter.api.AfterEach;
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
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateSupplierTokenWithSupplierId() throws Exception {
        givenToken("SUPPLIER", 7L);

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), new MockFilterChain());

        CustomUserPrincipal principal = (CustomUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.supplierId()).isEqualTo(7L);
    }

    @Test
    void shouldNotAuthenticateSupplierTokenWithoutSupplierId() throws Exception {
        givenToken("SUPPLIER", null);

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldAuthenticateNonSupplierTokenWithoutSupplierId() throws Exception {
        givenToken("ADMIN", null);

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    private void givenToken(String role, Long supplierId) {
        when(jwt.validateToken("token")).thenReturn(true);
        when(jwt.getUserIdFromToken("token")).thenReturn(3L);
        when(jwt.getEmailFromToken("token")).thenReturn("user@example.com");
        when(jwt.getRoleFromToken("token")).thenReturn(role);
        when(jwt.getSupplierIdFromToken("token")).thenReturn(supplierId);
    }

    private MockHttpServletRequest requestWithToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        return request;
    }
}
