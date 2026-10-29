package br.com.anhembi.supplychainverde.infrastructure.web;

import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class DashboardEmissionsIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Compara a view com a soma direto nas tabelas, no mês mais recente com emissão dos seeds.
    @Test
    void sumsWholeChainEmissionsByCalculationMonth() throws Exception {
        String month = latestEmissionMonth(null);

        mockMvc.perform(get("/api/v1/dashboard/emissions").param("from", month).param("to", month)
                        .with(authentication(as("ADMIN", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].month").value(month))
                .andExpect(jsonPath("$[0].co2Kg").value(expectedCo2(month, null).doubleValue()));
    }

    @Test
    void sumsOnlyLinkedSupplierEmissionsForSupplier() throws Exception {
        Long supplierId = jdbcTemplate.queryForObject("""
                SELECT b.supplier_id FROM carbon_emission ce
                JOIN chain c ON c.id = ce.chain_id JOIN batch b ON b.id = c.batch_id
                ORDER BY ce.created_at DESC LIMIT 1
                """, Long.class);
        String month = latestEmissionMonth(supplierId);

        mockMvc.perform(get("/api/v1/dashboard/emissions").param("from", month).param("to", month)
                        .with(authentication(as("SUPPLIER", supplierId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].co2Kg").value(expectedCo2(month, supplierId).doubleValue()));
    }

    // Passa pelas 5 consultas do resumo; o lote recente exercita a conversão de unit/status para enum.
    @Test
    void summarizesDashboardFromSeeds() throws Exception {
        long suppliers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM supplier", Long.class);

        mockMvc.perform(get("/api/v1/dashboard/summary").param("limit", "1")
                        .with(authentication(as("ADMIN", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suppliers").value(suppliers))
                .andExpect(jsonPath("$.activeBatches").isNumber())
                .andExpect(jsonPath("$.expiringCertifications").isNumber())
                .andExpect(jsonPath("$.monthlyEmissionKgCo2e").isNumber())
                .andExpect(jsonPath("$.recentBatches.length()").value(1))
                .andExpect(jsonPath("$.recentBatches[0].unit").isString())
                .andExpect(jsonPath("$.recentBatches[0].status").isString());
    }

    @Test
    void rejectsRangeLongerThanTwentyFourMonths() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/emissions").param("from", "2024-01").param("to", "2026-06")
                        .with(authentication(as("ADMIN", null))))
                .andExpect(status().isBadRequest());
    }

    private String latestEmissionMonth(Long supplierId) {
        return jdbcTemplate.queryForObject("""
                SELECT DATE_FORMAT(MAX(ce.created_at), '%Y-%m') FROM carbon_emission ce
                JOIN chain c ON c.id = ce.chain_id JOIN batch b ON b.id = c.batch_id
                WHERE ? IS NULL OR b.supplier_id = ?
                """, String.class, supplierId, supplierId);
    }

    private BigDecimal expectedCo2(String month, Long supplierId) {
        return jdbcTemplate.queryForObject("""
                SELECT SUM(ce.co2_kg) FROM carbon_emission ce
                JOIN chain c ON c.id = ce.chain_id JOIN batch b ON b.id = c.batch_id
                WHERE DATE_FORMAT(ce.created_at, '%Y-%m') = ? AND (? IS NULL OR b.supplier_id = ?)
                """, BigDecimal.class, month, supplierId, supplierId);
    }

    private UsernamePasswordAuthenticationToken as(String role, Long supplierId) {
        return new UsernamePasswordAuthenticationToken(
                new CustomUserPrincipal(1L, "actor@example.com", role, supplierId),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
