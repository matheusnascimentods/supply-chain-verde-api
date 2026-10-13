package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SupplierUserLinkIT {
    private static final String CNPJ = "11222333000181";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    // Sem @Transactional no teste: o rollback precisa ser o da própria requisição.
    @Test
    void rollsBackSupplierWhenLinkedUserEmailAlreadyExists() throws Exception {
        mockMvc.perform(post("/api/v1/suppliers")
                        .with(authentication(as("MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(supplierWithUser("m.neuer@bayern.com")))
                .andExpect(status().isConflict());

        Integer suppliers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM supplier WHERE cnpj = ?", Integer.class, CNPJ);
        assertThat(suppliers).isZero();
    }

    @Test
    @Transactional
    void createsSupplierAndLinkedSupplierUserTogether() throws Exception {
        mockMvc.perform(post("/api/v1/suppliers")
                        .with(authentication(as("MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(supplierWithUser("ana@fazendaverde.example.com")))
                .andExpect(status().isCreated());
        entityManager.flush(); // sem commit no teste, o UPDATE do vínculo ainda está pendente no Hibernate

        String role = jdbcTemplate.queryForObject("""
                SELECT u.role::TEXT FROM supplier s JOIN users u ON u.user_id = s.user_id WHERE s.cnpj = ?
                """, String.class, CNPJ);
        assertThat(role).isEqualTo("SUPPLIER");
    }

    private UsernamePasswordAuthenticationToken as(String role) {
        return new UsernamePasswordAuthenticationToken(
                new CustomUserPrincipal(1L, "actor@example.com", role, null),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    private String supplierWithUser(String email) {
        return """
                {
                  "name": "Fazenda Verde LTDA",
                  "cnpj": "%s",
                  "phone": "11999990000",
                  "address": { "street": "Rua A", "number": "10", "neighborhood": "Centro", "zipCode": "01001-000", "city": "São Paulo", "state": "SP" },
                  "user": { "name": "Ana Souza", "email": "%s", "password": "senha-forte" }
                }
                """.formatted(CNPJ, email);
    }
}
