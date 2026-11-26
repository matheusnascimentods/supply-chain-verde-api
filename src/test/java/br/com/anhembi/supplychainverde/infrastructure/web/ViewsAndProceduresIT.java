package br.com.anhembi.supplychainverde.infrastructure.web;

import br.com.anhembi.supplychainverde.TestPrincipals;
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

import static br.com.anhembi.supplychainverde.JdbcInserts.DEMO_MARKET;
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
        supplierId = insert(jdbcTemplate, """
                INSERT INTO company (name, cnpj, address_id, email, type)
                VALUES ('Fornecedor IT', '98765432000198', ?, 'fornecedor@it.test', 'SUPPLIER')
                """, addressId);
        productId = insert(jdbcTemplate,
                "INSERT INTO product (supplier_id, name, category, unit) VALUES (?, 'Café IT', 'AGRICULTURE', 'KG')",
                supplierId);
    }

    @Test
    void v22RemapsLegacyMethodsAndPreservesEmissionData() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM carbon_emission WHERE calculation_method IN ('IPCC', 'EMEP_EEA')",
                Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM emission_factor", Integer.class)).isEqualTo(18);

        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM carbon_emission ce
                JOIN chain c ON c.id = ce.chain_id
                JOIN batch b ON b.id = c.batch_id
                JOIN product p ON p.id = b.product_id
                WHERE c.stage_type = 'TRANSPORT'
                  AND p.unit IN ('KG', 'TON')
                  AND ce.cargo_weight_kg IS NULL
                """,
                Integer.class)).isZero();
        // RN-17: as 70 emissões IPCC/EMEP_EEA do seed foram remapeadas e auditadas, e nenhum UPDATE mexeu no co2_kg.
        assertThat(jdbcTemplate.queryForMap(
                """
                SELECT COUNT(*) AS remapped, COALESCE(SUM(JSON_CONTAINS_PATH(after_data, 'one', '$.co2_kg')), 0) AS co2_changed
                FROM audit_log
                WHERE affected_table = 'carbon_emission' AND action = 'UPDATE'
                  AND JSON_UNQUOTE(JSON_EXTRACT(before_data, '$.calculation_method')) IN ('IPCC', 'EMEP_EEA')
                """))
                .satisfies(row -> {
                    assertThat(((Number) row.get("remapped")).intValue()).isEqualTo(70);
                    assertThat(((Number) row.get("co2_changed")).intValue()).isZero();
                });
    }

    // Dois lotes do mesmo produto: 2 lotes, 1 produto. O relatório grava o retrato e a auditoria tem o autor.
    @Test
    void reportProcedureStoresTotalsOfThePeriod() throws Exception {
        batchWithEmission("12.5000");
        batchWithEmission("7.5000");
        LocalDate today = LocalDate.now();

        String body = mockMvc.perform(post("/api/v1/suppliers/{id}/reports", supplierId)
                        .with(authentication(asMarketManager()))
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
        jdbcTemplate.update("CALL sp_generate_sustainability_report(?, " + DEMO_MARKET + ", ?, ?, @report_id)",
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

    // RN-14 e RN-15 (019): o score desconta a intensidade (kg CO2e por tonelada) / 10, não o CO2 total.
    @Test
    void sustainabilityViewAppliesScoreFormula() {
        assertThat(score()).isEqualByComparingTo("100.00");

        certification("ACTIVE", LocalDate.now().plusYears(1));
        certification("EXPIRED", LocalDate.now().minusDays(1));
        // Lote sem peso: entra no CO2 total (RN-16), mas não na intensidade.
        batchWithEmission("1234.0000");
        assertThat(score()).isEqualByComparingTo("110.00");

        // 1000 kg CO2e em 2 t = 500 kg/t → 100 + 10 × 1 ativa − 50
        batchWithIntensity(supplierId, productId, "1000.0000", "2000");
        assertThat(score()).isEqualByComparingTo("60.00");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT total_co2_kg FROM vw_supplier_sustainability WHERE supplier_id = ?", BigDecimal.class, supplierId))
                .isEqualByComparingTo("2234");

        batchWithIntensity(supplierId, productId, "20000.0000", "1000");
        assertThat(score()).isEqualByComparingTo("0.00");
    }

    // Critério de aceite da 019: com menos kg CO2e por tonelada, o fornecedor fica à frente mesmo com mais CO2 total.
    @Test
    void rankingFollowsIntensityNotTotalCo2() {
        long efficient = supplierId;
        batchWithIntensity(efficient, productId, "2000.0000", "100000");  // 20 kg/t
        long addressId = jdbcTemplate.queryForObject("SELECT address_id FROM company WHERE id = ?", Long.class, efficient);
        long small = insert(jdbcTemplate, """
                INSERT INTO company (name, cnpj, address_id, email, type)
                VALUES ('Fornecedor IT pequeno', '98765432000200', ?, 'pequeno@it.test', 'SUPPLIER')
                """, addressId);
        long smallProduct = insert(jdbcTemplate,
                "INSERT INTO product (supplier_id, name, category, unit) VALUES (?, 'Milho IT', 'AGRICULTURE', 'KG')", small);
        batchWithIntensity(small, smallProduct, "100.0000", "1000");  // 100 kg/t

        assertThat(jdbcTemplate.queryForList("""
                SELECT supplier_id FROM vw_supplier_sustainability
                WHERE supplier_id IN (?, ?) ORDER BY sustainability_score DESC
                """, Long.class, efficient, small)).containsExactly(efficient, small);
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
        long batchId = insert(jdbcTemplate, """
                INSERT INTO batch (product_id, supplier_id, market_id, status, quantity, produced_at)
                VALUES (?, ?, %s, 'ACTIVE', 100, CURRENT_DATE)
                """.formatted(DEMO_MARKET), productId, supplierId);
        long chainId = insert(jdbcTemplate,
                "INSERT INTO chain (batch_id, destination_address_id, responsible_user_id, stage_type, started_at) "
                        + "VALUES (?, (SELECT c.address_id FROM company c JOIN batch b ON b.supplier_id = c.id WHERE b.id = ?), ?, 'PRODUCTION', NOW())",
                batchId, batchId, userId);
        jdbcTemplate.update(
                "INSERT INTO carbon_emission (chain_id, emission_factor, co2_kg, calculation_method) VALUES (?, 1, ?, 'GHG_PROTOCOL')",
                chainId, co2Kg);
    }

    /** Lote com as etapas até o TRANSPORT; a emissão dele traz o CO2 e o peso da carga. */
    private void batchWithIntensity(long supplier, long product, String co2Kg, String cargoWeightKg) {
        long batchId = insert(jdbcTemplate, """
                INSERT INTO batch (product_id, supplier_id, market_id, status, quantity, produced_at)
                VALUES (?, ?, %s, 'ACTIVE', 100, CURRENT_DATE)
                """.formatted(DEMO_MARKET), product, supplier);
        long chainId = 0;
        for (String stage : List.of("PRODUCTION", "STORAGE", "PROCESSING", "TRANSPORT")) {
            chainId = insert(jdbcTemplate,
                    "INSERT INTO chain (batch_id, destination_address_id, responsible_user_id, stage_type, started_at) "
                            + "VALUES (?, (SELECT address_id FROM company WHERE id = ?), ?, ?, NOW())",
                    batchId, supplier, userId, stage);
        }
        jdbcTemplate.update("""
                INSERT INTO carbon_emission (chain_id, emission_factor, co2_kg, calculation_method, cargo_weight_kg)
                VALUES (?, 1, ?, 'GHG_PROTOCOL', ?)
                """, chainId, co2Kg, cargoWeightKg);
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

    // RN-23: só o mercado gera relatório; o dos seeds é o mercado de demonstração.
    private UsernamePasswordAuthenticationToken asMarketManager() {
        Long marketId = jdbcTemplate.queryForObject("SELECT MIN(id) FROM company WHERE type = 'MARKET'", Long.class);
        return new UsernamePasswordAuthenticationToken(TestPrincipals.market(userId, "MANAGER", marketId), null,
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));
    }

    private UsernamePasswordAuthenticationToken as(String role) {
        return new UsernamePasswordAuthenticationToken(
                TestPrincipals.of(userId, "actor@example.com", role, null),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
