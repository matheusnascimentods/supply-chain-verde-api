package br.com.anhembi.supplychainverde.infrastructure.web;

import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static br.com.anhembi.supplychainverde.JdbcInserts.insert;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportDetailIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Período de um dia só: as emissões do fornecedor em outras datas dos seeds ficam de fora.
    @Test
    void sumsOnlyEmissionsInsideReportPeriod() throws Exception {
        Long supplierId = supplierWithEmissions();
        LocalDate day = jdbcTemplate.queryForObject(
                "SELECT MAX(calculated_at) FROM vw_stage_emission WHERE supplier_id = ?", LocalDate.class, supplierId);
        BigDecimal expected = co2(supplierId, "calculated_at = ?", day);
        assertThat(co2(supplierId, "calculated_at <> ?", day)).isPositive();
        Long reportId = insertReport(supplierId, day, day);

        String body = mockMvc.perform(get("/api/v1/reports").param("reportId", reportId.toString())
                        .with(authentication(as("AUDITOR", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCo2Kg").exists())
                .andReturn().getResponse().getContentAsString();

        assertThat(sum(body, "$.emissions.byStage[*].co2Kg")).isEqualByComparingTo(expected);
        assertThat(sum(body, "$.emissions.byBatch[*].co2Kg")).isEqualByComparingTo(expected);
        assertThat(sum(body, "$.emissions.byMethod[*].co2Kg")).isEqualByComparingTo(expected);
    }

    @Test
    void listsCertificationsValidAtSomePointOfThePeriod() throws Exception {
        Long supplierId = supplierWithEmissions();
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);
        insertCertification(supplierId, "Vencida antes", start.minusYears(2), start.minusDays(1));
        insertCertification(supplierId, "Vence no período", start.minusYears(1), start.plusMonths(2));
        insertCertification(supplierId, "Emitida depois", end.plusDays(1), end.plusYears(1));
        Long reportId = insertReport(supplierId, start, end);

        mockMvc.perform(get("/api/v1/reports").param("reportId", reportId.toString())
                        .with(authentication(as("SUPPLIER", supplierId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplier").exists())
                .andExpect(jsonPath("$.certifications[*].certification", hasItem("Vence no período")))
                .andExpect(jsonPath("$.certifications[*].certification", not(hasItem("Vencida antes"))))
                .andExpect(jsonPath("$.certifications[*].certification", not(hasItem("Emitida depois"))));
    }

    private Long supplierWithEmissions() {
        return jdbcTemplate.queryForObject("""
                SELECT supplier_id FROM vw_stage_emission
                GROUP BY supplier_id HAVING COUNT(DISTINCT calculated_at) > 1
                ORDER BY supplier_id LIMIT 1
                """, Long.class);
    }

    private BigDecimal co2(Long supplierId, String dateFilter, LocalDate day) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(co2_kg), 0) FROM vw_stage_emission WHERE supplier_id = ? AND " + dateFilter,
                BigDecimal.class, supplierId, day);
    }

    private Long insertReport(Long supplierId, LocalDate start, LocalDate end) {
        return insert(jdbcTemplate, """
                INSERT INTO report (supplier_id, period_start_at, period_end_at, total_co2_kg, tracked_product_count)
                VALUES (?, ?, ?, 0, 0)
                """, supplierId, start, end);
    }

    private void insertCertification(Long supplierId, String name, LocalDate issuedAt, LocalDate expiresAt) {
        jdbcTemplate.update("""
                INSERT INTO certification (supplier_id, certification, issuing_body, issued_at, expires_at, status)
                VALUES (?, ?, 'IBD', ?, ?, 'ACTIVE')
                """, supplierId, name, issuedAt, expiresAt);
    }

    private static BigDecimal sum(String body, String path) {
        List<Number> values = JsonPath.read(body, path);
        return values.stream().map(value -> new BigDecimal(value.toString())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private UsernamePasswordAuthenticationToken as(String role, Long supplierId) {
        return new UsernamePasswordAuthenticationToken(
                new CustomUserPrincipal(1L, "actor@example.com", role, supplierId),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
