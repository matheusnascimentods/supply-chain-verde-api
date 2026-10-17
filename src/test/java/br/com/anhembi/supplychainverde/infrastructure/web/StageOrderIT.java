package br.com.anhembi.supplychainverde.infrastructure.web;

import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import br.com.anhembi.supplychainverde.infrastructure.web.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ordem obrigatória das etapas (V39): validação na API e constraints no banco. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StageOrderIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long userId;
    private long batchId;

    @BeforeEach
    void setUp() {
        userId = jdbcTemplate.queryForObject("SELECT MIN(id) FROM users", Long.class);
        batchId = jdbcTemplate.queryForObject("""
                INSERT INTO batch (product_id, supplier_id, quantity, produced_at)
                VALUES ((SELECT MIN(id) FROM product), (SELECT MIN(id) FROM supplier), 10, CURRENT_DATE)
                RETURNING id
                """, Long.class);
    }

    @Test
    void apiAcceptsOnlyTheNextStage() throws Exception {
        postStage("STORAGE", 400);
        postStage("PRODUCTION", 201);
        postStage("PROCESSING", 400);
        postStage("PRODUCTION", 400);
        postStage("STORAGE", 201);
    }

    @Test
    void databaseRejectsOutOfOrderRepeatedAndMiddleDeletion() {
        long production = insertStage("PRODUCTION");

        assertViolates("fk_chain_previous_stage",
                () -> insertStage("PROCESSING"));
        assertViolates("uq_chain_batch_stage",
                () -> insertStage("PRODUCTION"));

        insertStage("STORAGE");
        assertViolates("fk_chain_previous_stage",
                () -> jdbcTemplate.update("DELETE FROM chain WHERE id = ?", production));
    }

    @Test
    void constraintViolationFromConcurrentRequestIsBadRequest() {
        var violation = new DataIntegrityViolationException("insert", new RuntimeException(
                "ERROR: duplicate key value violates unique constraint \"uq_chain_batch_stage\""));

        var response = new GlobalExceptionHandler().handleDataIntegrityViolation(violation);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).extractingByKey("message").asString().contains("Transição de etapa inválida");
    }

    private void postStage(String stageType, int expectedStatus) throws Exception {
        var result = mockMvc.perform(post("/api/v1/batches/{id}/stages", batchId)
                        .with(authentication(asManager()))
                        .header("X-Responsible-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageType\":\"" + stageType + "\",\"startedAt\":\"2026-01-01T10:00:00\"}"))
                .andExpect(status().is(expectedStatus));
        if (expectedStatus == 400) {
            result.andExpect(jsonPath("$.message").value(containsString("Transição de etapa inválida")));
        }
    }

    private long insertStage(String stageType) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO chain (batch_id, responsible_user_id, stage_type, started_at)
                VALUES (?, ?, ?::stage_type, NOW()) RETURNING id
                """, Long.class, batchId, userId, stageType);
    }

    // Erro aborta a transação do Postgres; o savepoint deixa o teste continuar na mesma transação.
    private void assertViolates(String constraint, Runnable statement) {
        jdbcTemplate.execute("SAVEPOINT before_violation");
        assertThatThrownBy(statement::run).hasMessageContaining(constraint);
        jdbcTemplate.execute("ROLLBACK TO SAVEPOINT before_violation");
    }

    private UsernamePasswordAuthenticationToken asManager() {
        return new UsernamePasswordAuthenticationToken(
                new CustomUserPrincipal(userId, "actor@example.com", "MANAGER", null),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));
    }
}
