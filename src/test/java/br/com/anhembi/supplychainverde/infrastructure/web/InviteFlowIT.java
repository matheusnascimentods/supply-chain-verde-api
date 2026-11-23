package br.com.anhembi.supplychainverde.infrastructure.web;

import br.com.anhembi.supplychainverde.TestPrincipals;
import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import br.com.anhembi.supplychainverde.application.exception.EmailDeliveryException;
import br.com.anhembi.supplychainverde.application.usecase.invite.InviteToken;
import br.com.anhembi.supplychainverde.domain.service.EmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static br.com.anhembi.supplychainverde.JdbcInserts.insert;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Feature 011 de ponta a ponta (plan §8), num mercado com teto de 3 assentos (016) e um único ADMIN. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InviteFlowIT {
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
                VALUES ('Rua do Convite', '1', 'Centro', '01000000', 'São Paulo', 'SP')
                """);
        companyId = insert(jdbcTemplate, """
                INSERT INTO company (name, cnpj, address_id, email, type, max_users)
                VALUES ('Mercado Convite', '98765432000198', ?, 'convite@it.test', 'MARKET', 3)
                """, address);
        adminId = insert(jdbcTemplate,
                "INSERT INTO users (name, email, password, role, company_id) VALUES ('Admin', 'admin@convite.test', 'x', 'ADMIN', ?)",
                companyId);
    }

    @Test
    void inviteConsultAcceptAndLogIn() throws Exception {
        String token = sendAndCaptureToken("ana@convite.test", "AUDITOR");

        mockMvc.perform(get("/api/v1/invites/{token}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@convite.test"))
                .andExpect(jsonPath("$.role").value("AUDITOR"))
                .andExpect(jsonPath("$.companyName").value("Mercado Convite"))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());

        accept(token).andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("AUDITOR"))
                .andExpect(jsonPath("$.companyId").value(companyId));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@convite.test\",\"password\":\"segredo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId").value(companyId));
    }

    @Test
    void anAcceptedTokenCannotBeReused() throws Exception {
        String token = sendAndCaptureToken("ana@convite.test", "AUDITOR");
        accept(token).andExpect(status().isOk());

        accept(token).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Convite inválido ou expirado."));
        mockMvc.perform(get("/api/v1/invites/{token}", token)).andExpect(status().isNotFound());
    }

    @Test
    void anExpiredTokenIsNotFound() throws Exception {
        String token = InviteToken.generate();
        jdbcTemplate.update("""
                INSERT INTO invite (invited_by, invited_for, role, token_hash, created_at)
                VALUES (?, 'velho@convite.test', 'AUDITOR', ?, NOW(6) - INTERVAL 25 HOUR)
                """, adminId, InviteToken.hash(token));

        mockMvc.perform(get("/api/v1/invites/{token}", token)).andExpect(status().isNotFound());
        accept(token).andExpect(status().isNotFound());
    }

    @Test
    void aSecondActiveInviteForTheSameEmailIsAConflict() throws Exception {
        sendAndCaptureToken("ana@convite.test", "AUDITOR");

        send("ana@convite.test", "MANAGER").andExpect(status().isConflict());
    }

    @Test
    void activeInvitesFillTheSeats() throws Exception {
        sendAndCaptureToken("um@convite.test", "AUDITOR");
        sendAndCaptureToken("dois@convite.test", "MANAGER");

        send("tres@convite.test", "AUDITOR").andExpect(status().isConflict());
    }

    // Fora da transação do teste: dentro dela, o rollback do use case só valeria no fim do teste.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void aFailedEmailIsABadGatewayAndSavesNothing() throws Exception {
        doThrow(new EmailDeliveryException("recusado", null)).when(emailSender).send(anyString(), anyString(), anyString());
        try {
            send("ana@convite.test", "AUDITOR").andExpect(status().isBadGateway());

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM invite WHERE invited_for = 'ana@convite.test'", Long.class)).isZero();
        } finally {
            long address = jdbcTemplate.queryForObject("SELECT address_id FROM company WHERE id = ?", Long.class, companyId);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", adminId);
            jdbcTemplate.update("DELETE FROM company WHERE id = ?", companyId);
            jdbcTemplate.update("DELETE FROM address WHERE id = ?", address);
        }
    }

    private String sendAndCaptureToken(String email, String role) throws Exception {
        send(email, role).andExpect(status().isCreated());
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(email), anyString(), html.capture());
        Matcher link = Pattern.compile("/convites/([A-Za-z0-9_-]{43})").matcher(html.getValue());
        assertThat(link.find()).isTrue();
        return link.group(1);
    }

    private ResultActions send(String email, String role) throws Exception {
        return mockMvc.perform(post("/api/v1/invites").with(asAdmin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"role\":\"" + role + "\"}"));
    }

    private ResultActions accept(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/invites/accept").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"name\":\"Ana\",\"password\":\"segredo\"}"));
    }

    private RequestPostProcessor asAdmin() {
        return authentication(new UsernamePasswordAuthenticationToken(TestPrincipals.market(adminId, "ADMIN", companyId), null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
}
