package br.com.anhembi.supplychainverde.infrastructure.web;

import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Feature 013 com a cadeia real do Spring Security: prova que o RateLimitFilter roda depois do JwtAuthenticationFilter.
 * A lógica de contagem fica no RateLimitFilterTest. O limite é ligado só aqui, num contexto próprio, e cada teste usa
 * um IP diferente para não consumir o limite do outro.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "rate-limit.enabled=true")
class RateLimitIT {
    private static final int GLOBAL_LIMIT = 120;
    // Rota pública fora dos limites estritos (GET /invites/ tem o seu). Um lote inexistente devolve 400; o que o
    // teste confere é só que a requisição passa (não é 429).
    private static final String PUBLIC_GET = "/api/v1/batches/999999999/traceability";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void twoUsersBehindTheSameIpHaveSeparateLimits() throws Exception {
        // Usuários reais do seed: o filtro JWT só autentica quem existe e está ativo (RN-11 da 016).
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM users WHERE company_id IS NOT NULL ORDER BY id LIMIT 2", Long.class);
        String ana = token(ids.get(0));
        String bruno = token(ids.get(1));

        for (int i = 0; i < GLOBAL_LIMIT; i++) {
            mockMvc.perform(publicGet("10.1.0.1", ana)).andExpect(status().isBadRequest());
        }

        mockMvc.perform(publicGet("10.1.0.1", ana))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
        mockMvc.perform(publicGet("10.1.0.1", bruno)).andExpect(status().isBadRequest());
    }

    @Test
    void theSixthLoginFromTheSameIpIsRejected() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(login("10.1.0.2")).andExpect(status().isUnauthorized());
        }

        mockMvc.perform(login("10.1.0.2"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.message").value("Muitas requisições. Tente novamente em instantes."));
    }

    // Um JWT inválido não autentica: a requisição conta pelo IP, junto com as que vêm sem token.
    @Test
    void anInvalidTokenCountsByIp() throws Exception {
        for (int i = 0; i < GLOBAL_LIMIT; i++) {
            mockMvc.perform(publicGet("10.1.0.3", "invalido")).andExpect(status().isBadRequest());
        }

        mockMvc.perform(get(PUBLIC_GET).with(ip("10.1.0.3"))).andExpect(status().isTooManyRequests());
    }

    private MockHttpServletRequestBuilder publicGet(String ip, String token) {
        return get(PUBLIC_GET).with(ip(ip)).header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private MockHttpServletRequestBuilder login(String ip) {
        return post("/api/v1/auth/login").with(ip(ip))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ninguem@rate.test\",\"password\":\"errada\"}");
    }

    private String token(long userId) {
        User user = User.builder().userId(userId).email("u" + userId + "@rate.test").role(UserRole.ADMIN).companyId(1L).build();
        return jwtTokenProvider.generateToken(user, CompanyType.MARKET);
    }

    private static RequestPostProcessor ip(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }
}
