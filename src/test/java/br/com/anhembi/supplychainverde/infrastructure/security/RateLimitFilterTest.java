package br.com.anhembi.supplychainverde.infrastructure.security;

import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {
    private final AtomicLong clock = new AtomicLong(1_000_000);
    private final RateLimitFilter filter = new RateLimitFilter(clock::get);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void theRequestAfterTheGlobalLimitIsRejectedWithoutReachingTheChain() throws Exception {
        sendAs(42L, "10.0.0.1", RateLimitFilter.GLOBAL_LIMIT);

        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        authenticate(42L);
        filter.doFilter(get("10.0.0.1", "/api/v1/plans"), response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("61");
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .isEqualTo("{\"message\":\"Muitas requisições. Tente novamente em instantes.\"}");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void twoUsersBehindTheSameIpHaveSeparateLimits() throws Exception {
        sendAs(42L, "10.0.0.1", RateLimitFilter.GLOBAL_LIMIT);

        assertThat(sendAs(42L, "10.0.0.1", 1)).isEqualTo(429);
        assertThat(sendAs(57L, "10.0.0.1", 1)).isEqualTo(200);
    }

    @Test
    void theSameUserIsLimitedEvenAfterChangingIp() throws Exception {
        sendAs(42L, "10.0.0.1", RateLimitFilter.GLOBAL_LIMIT);

        assertThat(sendAs(42L, "10.0.0.2", 1)).isEqualTo(429);
    }

    @Test
    void withoutAuthenticationTheLimitIsPerIp() throws Exception {
        for (int i = 0; i < RateLimitFilter.GLOBAL_LIMIT; i++) send(get("10.0.0.1", "/api/v1/plans"));

        assertThat(send(get("10.0.0.1", "/api/v1/plans"))).isEqualTo(429);
        assertThat(send(get("10.0.0.2", "/api/v1/plans"))).isEqualTo(200);
    }

    @Test
    void theSixthLoginFromTheSameIpIsRejectedWhileOtherRoutesPass() throws Exception {
        for (int i = 0; i < 5; i++) assertThat(send(post("10.0.0.1", "/api/v1/auth/login"))).isEqualTo(200);

        assertThat(send(post("10.0.0.1", "/api/v1/auth/login"))).isEqualTo(429);
        assertThat(send(get("10.0.0.1", "/api/v1/plans"))).isEqualTo(200);
        assertThat(send(post("10.0.0.2", "/api/v1/auth/login"))).isEqualTo(200);
    }

    // RN-04: o limite estrito vale por IP mesmo com JWT, então trocar de usuário não ganha tentativas.
    @Test
    void theStrictLimitCountsTheIpEvenWhenAuthenticated() throws Exception {
        for (int i = 0; i < 3; i++) {
            authenticate(100L + i);
            send(post("10.0.0.1", "/api/v1/companies"));
        }

        authenticate(200L);
        assertThat(send(post("10.0.0.1", "/api/v1/companies"))).isEqualTo(429);
    }

    @Test
    void differentInviteTokensShareTheSameStrictRule() throws Exception {
        for (String token : List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")) {
            assertThat(send(get("10.0.0.1", "/api/v1/invites/" + token))).isEqualTo(200);
        }

        assertThat(send(get("10.0.0.1", "/api/v1/invites/k"))).isEqualTo(429);
    }

    @Test
    void aBlockedClientPassesAgainWhenTheWindowTurns() throws Exception {
        for (int i = 0; i < 5; i++) send(post("10.0.0.1", "/api/v1/auth/login"));
        assertThat(send(post("10.0.0.1", "/api/v1/auth/login"))).isEqualTo(429);

        clock.addAndGet(RateLimitFilter.WINDOW_MS);

        assertThat(send(post("10.0.0.1", "/api/v1/auth/login"))).isEqualTo(200);
    }

    @Test
    void retryAfterShrinksAsTheWindowAdvances() throws Exception {
        for (int i = 0; i < 5; i++) send(post("10.0.0.1", "/api/v1/auth/login"));
        clock.addAndGet(45_000);

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(post("10.0.0.1", "/api/v1/auth/login"), response, new MockFilterChain());

        assertThat(response.getHeader("Retry-After")).isEqualTo("16");
    }

    private int sendAs(long userId, String ip, int times) throws Exception {
        int status = 0;
        for (int i = 0; i < times; i++) {
            authenticate(userId);
            status = send(get(ip, "/api/v1/plans"));
        }
        return status;
    }

    private int send(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }

    private static void authenticate(long userId) {
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, "u" + userId + "@test", "ADMIN", 1L, CompanyType.MARKET);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private static MockHttpServletRequest get(String ip, String uri) {
        return request("GET", ip, uri);
    }

    private static MockHttpServletRequest post(String ip, String uri) {
        return request("POST", ip, uri);
    }

    private static MockHttpServletRequest request(String method, String ip, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(ip);
        return request;
    }
}
