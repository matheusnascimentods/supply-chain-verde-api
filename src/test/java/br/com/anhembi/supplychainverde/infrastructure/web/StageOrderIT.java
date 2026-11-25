package br.com.anhembi.supplychainverde.infrastructure.web;

import br.com.anhembi.supplychainverde.TestPrincipals;
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

import static br.com.anhembi.supplychainverde.JdbcInserts.SEED_MARKET;
import static br.com.anhembi.supplychainverde.JdbcInserts.insert;
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
    private long supplierId;
    private long batchId;

    @BeforeEach
    void setUp() {
        // Etapas antes do RETAIL são do fornecedor (RN-08): quem age e o responsável são do fornecedor do lote.
        long productId = jdbcTemplate.queryForObject("""
                SELECT MIN(p.id) FROM product p
                WHERE EXISTS (SELECT 1 FROM users u WHERE u.company_id = p.supplier_id AND u.is_active)
                """, Long.class);
        supplierId = jdbcTemplate.queryForObject("SELECT supplier_id FROM product WHERE id = ?", Long.class, productId);
        userId = jdbcTemplate.queryForObject(
                "SELECT MIN(id) FROM users WHERE company_id = ? AND is_active", Long.class, supplierId);
        batchId = insert(jdbcTemplate, """
                INSERT INTO batch (product_id, supplier_id, market_id, status, quantity, produced_at)
                VALUES (?, ?, %s, 'ACTIVE', 10, CURRENT_DATE)
                """.formatted(SEED_MARKET), productId, supplierId);
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
                "Duplicate entry '1-PRODUCTION' for key 'chain.uq_chain_batch_stage'"));

        var response = new GlobalExceptionHandler().handleDataIntegrityViolation(violation);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).extractingByKey("message").asString().contains("Transição de etapa inválida");
    }

    private void postStage(String stageType, int expectedStatus) throws Exception {
        var result = mockMvc.perform(post("/api/v1/batches/{id}/stages", batchId)
                        .with(authentication(asSupplier()))
                        .header("X-Responsible-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageType\":\"" + stageType + "\",\"startedAt\":\"2026-01-01T10:00:00\",\"destination\":{\"street\":\"Rua A\",\"number\":\"1\",\"neighborhood\":\"Centro\",\"zipCode\":\"13000000\",\"city\":\"Campinas\",\"state\":\"SP\"}}"))
                .andExpect(status().is(expectedStatus));
        if (expectedStatus == 400) {
            result.andExpect(jsonPath("$.message").value(containsString("Transição de etapa inválida")));
        }
    }

    private long insertStage(String stageType) {
        return insert(jdbcTemplate, """
                INSERT INTO chain (batch_id, responsible_user_id, stage_type, started_at)
                VALUES (?, ?, ?, NOW())
                """, batchId, userId, stageType);
    }

    // No MySQL o erro desfaz só a instrução; a transação do teste continua.
    private void assertViolates(String constraint, Runnable statement) {
        assertThatThrownBy(statement::run).hasMessageContaining(constraint);
    }

    private UsernamePasswordAuthenticationToken asSupplier() {
        return new UsernamePasswordAuthenticationToken(
                TestPrincipals.of(userId, "supplier@example.com", "SUPPLIER", supplierId),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_SUPPLIER")));
    }
}
