package br.com.anhembi.supplychainverde.infrastructure.web;

import br.com.anhembi.supplychainverde.TestPrincipals;
import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import br.com.anhembi.supplychainverde.domain.service.EmailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static br.com.anhembi.supplychainverde.JdbcInserts.insert;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RN-04 e RN-08 de ponta a ponta, num mercado BASIC (3 usuários) com um único ADMIN. Usuários só entram por convite
 * (feature 011); o limite ocupado por convites ativos fica no InviteFlowIT.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserPlanLimitIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailSender emailSender;

    private long companyId;
    private long adminId;

    @BeforeEach
    void setUp() {
        long address = insert(jdbcTemplate, """
                INSERT INTO address (street, number, neighborhood, zip_code, city, state)
                VALUES ('Rua do Plano', '1', 'Centro', '01000000', 'São Paulo', 'SP')
                """);
        companyId = insert(jdbcTemplate, """
                INSERT INTO company (name, cnpj, address_id, email, type)
                VALUES ('Mercado Basic', '98765432000198', ?, 'basic@plano.test', 'MARKET')
                """, address);
        jdbcTemplate.update("INSERT INTO company_plan (company_id, plan_id) SELECT ?, id FROM plan WHERE code = 'BASIC'", companyId);
        adminId = insert(jdbcTemplate,
                "INSERT INTO users (name, email, password, role, company_id) VALUES ('Admin', 'admin@plano.test', 'x', 'ADMIN', ?)",
                companyId);
    }

    @Test
    void aBasicPlanWithThreeUsersCannotInviteAFourth() throws Exception {
        for (String email : List.of("um@plano.test", "dois@plano.test")) {
            insert(jdbcTemplate, "INSERT INTO users (name, email, password, role, company_id) VALUES ('U', ?, 'x', 'AUDITOR', ?)",
                    email, companyId);
        }

        invite("tres@plano.test", "AUDITOR").andExpect(status().isConflict());
        verifyNoInteractions(emailSender);
    }

    @Test
    void aRoleThatTheMarketDoesNotAllowIsABadRequest() throws Exception {
        invite("fornecedor@plano.test", "SUPPLIER").andExpect(status().isBadRequest());
    }

    @Test
    void theOnlyAdminCannotDemoteHimself() throws Exception {
        mockMvc.perform(patch("/api/v1/users/{id}/role", adminId).with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"MANAGER\"}"))
                .andExpect(status().isConflict());
    }

    private org.springframework.test.web.servlet.ResultActions invite(String email, String role) throws Exception {
        return mockMvc.perform(post("/api/v1/invites").with(asAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"role\":\"" + role + "\"}"));
    }

    private RequestPostProcessor asAdmin() {
        return authentication(new UsernamePasswordAuthenticationToken(TestPrincipals.market(adminId, "ADMIN", companyId), null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
}
