package br.com.anhembi.supplychainverde.infrastructure.audit;

import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static br.com.anhembi.supplychainverde.JdbcInserts.insert;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "spring.datasource.hikari.maximum-pool-size=1")
class AuditLogTriggerIT {
    @Autowired
    private AuditActorContextAspect actorContextAspect;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void capturesInsertChangedFieldsAndDeleteWithTransactionActor() throws Exception {
        Long actorId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?",
                Long.class,
                "m.neuer@bayern.com"
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CustomUserPrincipal(actorId, "m.neuer@bayern.com", "ADMIN", null),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));
        actorContextAspect.setAuthenticatedActorForTransaction();

        Long addressId = insert(jdbcTemplate, """
                INSERT INTO address (street, number, neighborhood, zip_code, city, state)
                VALUES ('Rua Um', '10', 'Centro', '01000-000', 'Santos', 'SP')
                """);
        jdbcTemplate.update("UPDATE address SET city = city WHERE id = ?", addressId);
        jdbcTemplate.update("UPDATE address SET city = ? WHERE id = ?", "Campinas", addressId);
        jdbcTemplate.update("DELETE FROM address WHERE id = ?", addressId);

        List<AuditEvent> events = jdbcTemplate.query("""
                SELECT action, user_id, affected_entity_id, before_data, after_data
                FROM audit_log
                WHERE affected_table = 'address' AND affected_entity_id = ?
                ORDER BY id
                """, (result, rowNumber) -> new AuditEvent(
                result.getString("action"),
                result.getObject("user_id", Long.class),
                result.getLong("affected_entity_id"),
                result.getString("before_data"),
                result.getString("after_data")
        ), addressId);

        assertThat(events).hasSize(3);
        assertThat(events).extracting(AuditEvent::action).containsExactly("INSERT", "UPDATE", "DELETE");
        assertThat(events).allSatisfy(event -> {
            assertThat(event.userId()).isEqualTo(actorId);
            assertThat(event.affectedEntityId()).isEqualTo(addressId);
        });

        JsonNode inserted = objectMapper.readTree(events.getFirst().afterData());
        assertThat(inserted.path("city").asText()).isEqualTo("Santos");
        assertThat(inserted.path("id").asLong()).isEqualTo(addressId);
        assertThat(inserted.has("password")).isFalse();

        JsonNode beforeUpdate = objectMapper.readTree(events.get(1).beforeData());
        JsonNode afterUpdate = objectMapper.readTree(events.get(1).afterData());
        assertThat(beforeUpdate.size()).isEqualTo(1);
        assertThat(beforeUpdate.path("city").asText()).isEqualTo("Santos");
        assertThat(afterUpdate.size()).isEqualTo(1);
        assertThat(afterUpdate.path("city").asText()).isEqualTo("Campinas");

        JsonNode deleted = objectMapper.readTree(events.get(2).beforeData());
        assertThat(deleted.path("city").asText()).isEqualTo("Campinas");
        assertThat(events.get(2).afterData()).isNull();

        Long supplierId = jdbcTemplate.queryForObject("SELECT id FROM supplier ORDER BY id LIMIT 1", Long.class);
        Long certificationId = insert(jdbcTemplate, """
                INSERT INTO certification (supplier_id, certification, issuing_body, issued_at, expires_at, status)
                VALUES (?, 'Teste de auditoria', 'Laboratório', CURRENT_DATE, CURRENT_DATE + INTERVAL 30 DAY, 'ACTIVE')
                """, supplierId);
        jdbcTemplate.update("UPDATE certification SET status = 'SUSPENDED' WHERE id = ?", certificationId);

        List<AuditEvent> certificationEvents = jdbcTemplate.query("""
                SELECT action, user_id, affected_entity_id, before_data, after_data
                FROM audit_log
                WHERE affected_table = 'certification' AND affected_entity_id = ?
                ORDER BY id
                """, (result, rowNumber) -> new AuditEvent(
                result.getString("action"),
                result.getObject("user_id", Long.class),
                result.getLong("affected_entity_id"),
                result.getString("before_data"),
                result.getString("after_data")
        ), certificationId);
        assertThat(certificationEvents).extracting(AuditEvent::action)
                .containsExactly("INSERT", "STATUS_CHANGE");
        assertThat(objectMapper.readTree(certificationEvents.get(1).beforeData()).path("status").asText())
                .isEqualTo("ACTIVE");
        assertThat(objectMapper.readTree(certificationEvents.get(1).afterData()).path("status").asText())
                .isEqualTo("SUSPENDED");

        mockMvc.perform(get("/api/v1/audit-logs")
                        .param("from", LocalDate.now().toString())
                        .param("to", LocalDate.now().toString())
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                new CustomUserPrincipal(actorId, "m.neuer@bayern.com", "ADMIN", null),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_AUDITOR"))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].action").value("STATUS_CHANGE"))
                .andExpect(jsonPath("$.items[0].affectedEntityId").value(certificationId))
                .andExpect(jsonPath("$.items[0].userId").value(actorId))
                .andExpect(jsonPath("$.items[0].beforeData.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].afterData.status").value("SUSPENDED"));
    }

    @Test
    void touchesUpdatedAtOnlyWhenTheRowChanges() {
        Long addressId = insert(jdbcTemplate, """
                INSERT INTO address (street, number, neighborhood, zip_code, city, state, created_at, updated_at)
                VALUES ('Rua Dois', '20', 'Centro', '01000-000', 'Santos', 'SP', '2020-01-01', '2020-01-01')
                """);
        String updatedAt = "SELECT updated_at FROM address WHERE id = ?";
        String original = jdbcTemplate.queryForObject(updatedAt, String.class, addressId);

        jdbcTemplate.update("UPDATE address SET city = city WHERE id = ?", addressId);
        assertThat(jdbcTemplate.queryForObject(updatedAt, String.class, addressId)).isEqualTo(original);

        jdbcTemplate.update("UPDATE address SET city = 'Campinas' WHERE id = ?", addressId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT updated_at > '2020-01-01' FROM address WHERE id = ?", Boolean.class, addressId)).isTrue();
    }

    @Test
    void appUserCannotWriteAuditLogDirectly() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO audit_log (action, affected_table, affected_entity_id) VALUES ('INSERT', 'address', 1)"))
                .rootCause().hasMessageContaining("INSERT command denied");
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM audit_log"))
                .rootCause().hasMessageContaining("DELETE command denied");
    }

    // @app_user_id é da sessão: com pool de uma conexão, a escrita seguinte sem aspect não pode herdar o ator.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void doesNotLeakActorToTheNextWriteOnTheSameConnection() {
        Long actorId = jdbcTemplate.queryForObject("SELECT MIN(id) FROM users", Long.class);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CustomUserPrincipal(actorId, "actor@example.com", "ADMIN", null), null, List.of()));
        Long addressId = transactionTemplate.execute(status -> {
            actorContextAspect.setAuthenticatedActorForTransaction();
            return insert(jdbcTemplate, """
                    INSERT INTO address (street, number, neighborhood, zip_code, city, state)
                    VALUES ('Rua Três', '30', 'Centro', '01000-000', 'Santos', 'SP')
                    """);
        });
        SecurityContextHolder.clearContext();

        try {
            transactionTemplate.executeWithoutResult(status ->
                    jdbcTemplate.update("UPDATE address SET city = 'Campinas' WHERE id = ?", addressId));

            List<Long> actors = jdbcTemplate.queryForList("""
                    SELECT user_id FROM audit_log WHERE affected_table = 'address' AND affected_entity_id = ? ORDER BY id
                    """, Long.class, addressId);
            assertThat(actors).containsExactly(actorId, null);
        } finally {
            jdbcTemplate.update("DELETE FROM address WHERE id = ?", addressId);
        }
    }

    private record AuditEvent(
            String action,
            Long userId,
            Long affectedEntityId,
            String beforeData,
            String afterData
    ) { }
}
