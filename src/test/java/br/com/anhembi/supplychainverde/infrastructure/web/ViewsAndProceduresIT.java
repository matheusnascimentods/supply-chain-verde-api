package br.com.anhembi.supplychainverde.infrastructure.web;

import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static br.com.anhembi.supplychainverde.JdbcInserts.insert;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Views e procedures da V36–V38 contra um cenário montado no próprio teste. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ViewsAndProceduresIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MySQLContainer mysql;

    private long userId;
    private long supplierId;
    private long productId;

    @BeforeEach
    void setUp() {
        userId = jdbcTemplate.queryForObject("SELECT MIN(id) FROM users", Long.class);
        long addressId = insert(jdbcTemplate, """
                INSERT INTO address (street, number, neighborhood, zip_code, city, state)
                VALUES ('Rua Teste', '1', 'Centro', '01000000', 'São Paulo', 'SP')
                """);
        supplierId = insert(jdbcTemplate,
                "INSERT INTO supplier (name, cnpj, address_id) VALUES ('Fornecedor IT', '98765432000198', ?)", addressId);
        productId = insert(jdbcTemplate,
                "INSERT INTO product (name, category, unit) VALUES ('Café IT', 'AGRICULTURE', 'KG')");
    }

    // Dois lotes do mesmo produto: 2 lotes, 1 produto. O relatório grava o retrato e a auditoria tem o autor.
    @Test
    void reportProcedureStoresTotalsOfThePeriod() throws Exception {
        batchWithEmission("12.5000");
        batchWithEmission("7.5000");
        LocalDate today = LocalDate.now();

        String body = mockMvc.perform(post("/api/v1/suppliers/{id}/reports", supplierId)
                        .with(authentication(as("MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"periodStartAt\":\"" + today.minusDays(1) + "\",\"periodEndAt\":\"" + today + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reportId = ((Number) JsonPath.read(body, "$.reportId")).longValue();
        var report = jdbcTemplate.queryForMap(
                "SELECT total_co2_kg, batch_count, tracked_product_count FROM report WHERE id = ?", reportId);
        assertThat((BigDecimal) report.get("total_co2_kg")).isEqualByComparingTo("20");
        assertThat(report.get("batch_count")).isEqualTo(2);
        assertThat(report.get("tracked_product_count")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT user_id FROM audit_log WHERE affected_table = 'report' AND affected_entity_id = ? AND action = 'INSERT'",
                Long.class, reportId)).isEqualTo(userId);

        // Fora do período: relatório zerado, não erro.
        jdbcTemplate.update("CALL sp_generate_sustainability_report(?, ?, ?, @report_id)",
                supplierId, LocalDate.of(1990, 1, 1), LocalDate.of(1990, 12, 31));
        Long empty = jdbcTemplate.queryForObject("SELECT @report_id", Long.class);
        assertThat(jdbcTemplate.queryForObject("SELECT total_co2_kg FROM report WHERE id = ?", BigDecimal.class, empty))
                .isEqualByComparingTo("0");
    }

    @Test
    void expireProcedureOnlyTouchesActiveExpiredAndIsScheduled() {
        long expired = certification("ACTIVE", LocalDate.now().minusDays(1));
        long suspended = certification("SUSPENDED", LocalDate.now().minusDays(1));
        long valid = certification("ACTIVE", LocalDate.now());

        jdbcTemplate.execute("CALL sp_expire_certifications(@expired_count)");

        assertThat(certificationStatus(expired)).isEqualTo("EXPIRED");
        assertThat(certificationStatus(suspended)).isEqualTo("SUSPENDED");
        assertThat(certificationStatus(valid)).isEqualTo("ACTIVE");
        assertThat(jdbcTemplate.queryForObject("SELECT @expired_count", Integer.class)).isPositive();
        // supply_app não enxerga events em information_schema; a leitura é como root.
        var root = new JdbcTemplate(new DriverManagerDataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()));
        assertThat(root.queryForMap("""
                SELECT interval_value, interval_field, TIME_FORMAT(starts, '%H:%i') AS starts_at, status FROM information_schema.events
                WHERE event_schema = 'supply_chain_verde' AND event_name = 'ev_expire_certifications'
                """)).containsEntry("interval_value", "1").containsEntry("interval_field", "DAY")
                .containsEntry("starts_at", "00:00").containsEntry("status", "ENABLED");
    }

    @Test
    void sustainabilityViewAppliesScoreFormula() {
        assertThat(score()).isEqualByComparingTo("100.00");

        certification("ACTIVE", LocalDate.now().plusYears(1));
        certification("EXPIRED", LocalDate.now().minusDays(1));
        batchWithEmission("1234.0000");
        // 100 + 10 × 1 ativa − 12,34
        assertThat(score()).isEqualByComparingTo("97.66");

        batchWithEmission("20000.0000");
        assertThat(score()).isEqualByComparingTo("0.00");
    }

    @Test
    void rejectsExpiredCertificationRules() throws Exception {
        mockMvc.perform(post("/api/v1/suppliers/{id}/certifications", supplierId)
                        .with(authentication(as("ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"certification\":\"Orgânico\",\"issuingBody\":\"IBD\",\"issuedAt\":\"2020-01-01\",\"expiresAt\":\""
                                + LocalDate.now().minusDays(1) + "\"}"))
                .andExpect(status().isBadRequest());

        long expired = certification("EXPIRED", LocalDate.now().minusDays(1));
        mockMvc.perform(patch("/api/v1/certifications/{id}/status", expired)
                        .with(authentication(as("ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isBadRequest());
        assertThat(certificationStatus(expired)).isEqualTo("EXPIRED");
    }

    private void batchWithEmission(String co2Kg) {
        long batchId = insert(jdbcTemplate,
                "INSERT INTO batch (product_id, supplier_id, quantity, produced_at) VALUES (?, ?, 100, CURRENT_DATE)",
                productId, supplierId);
        long chainId = insert(jdbcTemplate,
                "INSERT INTO chain (batch_id, responsible_user_id, stage_type, started_at) VALUES (?, ?, 'PRODUCTION', NOW())",
                batchId, userId);
        jdbcTemplate.update(
                "INSERT INTO carbon_emission (chain_id, emission_factor, co2_kg, calculation_method) VALUES (?, 1, ?, 'IPCC')",
                chainId, co2Kg);
    }

    private long certification(String status, LocalDate expiresAt) {
        return insert(jdbcTemplate, """
                INSERT INTO certification (supplier_id, certification, issuing_body, issued_at, expires_at, status)
                VALUES (?, 'Certificação IT', 'IBD', DATE '2020-01-01', ?, ?)
                """, supplierId, expiresAt, status);
    }

    private String certificationStatus(long certificationId) {
        return jdbcTemplate.queryForObject("SELECT status FROM certification WHERE id = ?", String.class, certificationId);
    }

    private BigDecimal score() {
        return jdbcTemplate.queryForObject(
                "SELECT sustainability_score FROM vw_supplier_sustainability WHERE supplier_id = ?", BigDecimal.class, supplierId);
    }

    private UsernamePasswordAuthenticationToken as(String role) {
        return new UsernamePasswordAuthenticationToken(
                new CustomUserPrincipal(userId, "actor@example.com", role, null),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
