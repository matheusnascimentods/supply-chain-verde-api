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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UpdateOwnUserIT {
    private static final String EMAIL = "m.neuer@bayern.com";
    private static final String SEED_PASSWORD = "rolocompressor06";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void adminChangesOwnNameAndPasswordAndLogsInWithTheNewOne() throws Exception {
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, EMAIL);
        var admin = authentication(new UsernamePasswordAuthenticationToken(
                new CustomUserPrincipal(userId, EMAIL, "ADMIN", null), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        mockMvc.perform(patch("/api/v1/users/me").with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\": \"Manuel N.\", \"currentPassword\": \"" + SEED_PASSWORD
                                + "\", \"newPassword\": \"senha-nova-123\", \"email\": \"outro@example.com\", \"role\": \"AUDITOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.name").value("Manuel N."))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(get("/api/v1/users/me").with(admin))
                .andExpect(jsonPath("$.name").value("Manuel N."));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + EMAIL + "\", \"password\": \"senha-nova-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());

        Map<String, Object> event = jdbcTemplate.queryForMap("""
                SELECT user_id, CAST(after_data AS CHAR) AS after_data FROM audit_log
                WHERE affected_table = 'users' AND affected_entity_id = ? AND action = 'UPDATE'
                ORDER BY id DESC LIMIT 1
                """, userId);
        assertThat(event.get("user_id")).isEqualTo(userId);
        assertThat((String) event.get("after_data")).contains("Manuel N.").doesNotContain("password");
    }

    @Test
    void wrongCurrentPasswordChangesNothing() throws Exception {
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, EMAIL);
        String nameBefore = jdbcTemplate.queryForObject("SELECT name FROM users WHERE id = ?", String.class, userId);

        mockMvc.perform(patch("/api/v1/users/me")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                new CustomUserPrincipal(userId, EMAIL, "AUDITOR", null), null,
                                List.of(new SimpleGrantedAuthority("ROLE_AUDITOR")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\": \"Não Grava\", \"currentPassword\": \"errada\", \"newPassword\": \"senha-nova-123\"}"))
                .andExpect(status().isBadRequest());

        assertThat(jdbcTemplate.queryForObject("SELECT name FROM users WHERE id = ?", String.class, userId)).isEqualTo(nameBefore);
    }
}
