package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SupplierOwnProfileIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deniesManager() throws Exception {
        mockMvc.perform(patch("/api/v1/suppliers/me")
                        .with(authentication(principal("MANAGER", null, null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Outra Razão\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void updatesLinkedSupplierOfSupplierUser() throws Exception {
        Long supplierId = linkedSupplierId();

        mockMvc.perform(patch("/api/v1/suppliers/me")
                        .with(authentication(principal("SUPPLIER", linkedUserId(supplierId), supplierId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Razão Atualizada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplierId").value(supplierId))
                .andExpect(jsonPath("$.name").value("Razão Atualizada"));
    }

    // Sem @Transactional no teste: confere no banco que nada foi gravado.
    @Test
    void keepsEverythingWhenCurrentPasswordIsWrong() throws Exception {
        Long supplierId = linkedSupplierId();
        String nameBefore = supplierName(supplierId);

        mockMvc.perform(patch("/api/v1/suppliers/me")
                        .with(authentication(principal("SUPPLIER", linkedUserId(supplierId), supplierId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Não Deve Gravar\", \"currentPassword\": \"errada\", \"newPassword\": \"nova-senha\"}"))
                .andExpect(status().isBadRequest());

        assertThat(supplierName(supplierId)).isEqualTo(nameBefore);
    }

    private Long linkedSupplierId() {
        return jdbcTemplate.queryForObject(
                "SELECT supplier_id FROM supplier WHERE user_id IS NOT NULL ORDER BY supplier_id LIMIT 1", Long.class);
    }

    private Long linkedUserId(Long supplierId) {
        return jdbcTemplate.queryForObject("SELECT user_id FROM supplier WHERE supplier_id = ?", Long.class, supplierId);
    }

    private String supplierName(Long supplierId) {
        return jdbcTemplate.queryForObject("SELECT name FROM supplier WHERE supplier_id = ?", String.class, supplierId);
    }

    private UsernamePasswordAuthenticationToken principal(String role, Long userId, Long supplierId) {
        return new UsernamePasswordAuthenticationToken(
                new CustomUserPrincipal(userId == null ? 1L : userId, "actor@example.com", role, supplierId),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
