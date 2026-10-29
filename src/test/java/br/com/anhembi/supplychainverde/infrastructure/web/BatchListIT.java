package br.com.anhembi.supplychainverde.infrastructure.web;

import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sem @Transactional de propósito: com a sessão aberta pelo teste, os proxies preguiçosos carregariam
 * e o LazyInitializationException do max_fetch_depth=2 do MySQLDialect não apareceria.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class BatchListIT {
    @Autowired
    private MockMvc mockMvc;

    // Os seeds têm etapas com transporte: o grafo transport → chain → batch → supplier → address precisa vir inteiro.
    @Test
    void listsBatchesWithTransportedStages() throws Exception {
        mockMvc.perform(get("/api/v1/batches").param("size", "100")
                        .with(authentication(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].stages[*].transport", hasItem(notNullValue())));
    }

    private UsernamePasswordAuthenticationToken asAdmin() {
        return new UsernamePasswordAuthenticationToken(
                new CustomUserPrincipal(1L, "actor@example.com", "ADMIN", null),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
