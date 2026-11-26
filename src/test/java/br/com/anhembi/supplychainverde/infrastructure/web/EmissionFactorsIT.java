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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class EmissionFactorsIT {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void authenticatedUserListsAllEmissionFactors() throws Exception {
        mockMvc.perform(get("/api/v1/emission-factors").with(authentication(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(18)))
                .andExpect(jsonPath("$[0].calculationMethod").exists())
                .andExpect(jsonPath("$[0].transportMode").exists())
                .andExpect(jsonPath("$[0].value").exists());
    }

    @Test
    void unauthenticatedUserCannotListEmissionFactors() throws Exception {
        mockMvc.perform(get("/api/v1/emission-factors"))
                .andExpect(status().isUnauthorized());
    }

    private UsernamePasswordAuthenticationToken asAdmin() {
        var principal = new CustomUserPrincipal(1L, "admin@example.com", "ADMIN", null, null);
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }
}
