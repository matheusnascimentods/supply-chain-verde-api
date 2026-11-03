package br.com.anhembi.supplychainverde.infrastructure.web;

import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static br.com.anhembi.supplychainverde.JdbcInserts.insert;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Dois mercados e um fornecedor com lote só no mercado A: nenhum mercado enxerga o outro (RN-12 a RN-14). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TenantIsolationIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long marketA;
    private long marketB;
    private long supplier;
    private long batchA;
    private long reportA;
    private long adminA;
    private long auditorA;
    private long adminB;
    private long supplierUser;

    @BeforeEach
    void setUp() {
        long address = insert(jdbcTemplate, """
                INSERT INTO address (street, number, neighborhood, zip_code, city, state)
                VALUES ('Rua Isolada', '1', 'Centro', '01000000', 'São Paulo', 'SP')
                """);
        marketA = company("Mercado A", "98765432000198", address, "MARKET");
        marketB = company("Mercado B", "98765432000279", address, "MARKET");
        supplier = company("Fornecedor Compartilhado", "98765432000350", address, "SUPPLIER");
        long product = insert(jdbcTemplate,
                "INSERT INTO product (supplier_id, name, category, unit) VALUES (?, 'Produto isolado', 'OTHER', 'KG')", supplier);
        batchA = insert(jdbcTemplate, """
                INSERT INTO batch (product_id, supplier_id, market_id, status, quantity, produced_at)
                VALUES (?, ?, ?, 'ACTIVE', 10, CURRENT_DATE)
                """, product, supplier, marketA);
        reportA = insert(jdbcTemplate, """
                INSERT INTO report (supplier_id, market_id, period_start_at, period_end_at, total_co2_kg, tracked_product_count)
                VALUES (?, ?, DATE '2026-01-01', DATE '2026-03-31', 0, 0)
                """, supplier, marketA);
        adminA = user("admin.a@isolamento.test", "ADMIN", marketA);
        auditorA = user("auditor.a@isolamento.test", "AUDITOR", marketA);
        adminB = user("admin.b@isolamento.test", "ADMIN", marketB);
        supplierUser = user("fornecedor@isolamento.test", "SUPPLIER", supplier);
    }

    @Test
    void eachMarketSeesOnlyItsOwnBatchesAndReportsAndThePlatformAdminSeesAll() throws Exception {
        mockMvc.perform(get("/api/v1/batches").param("size", "100").with(as(adminA, "ADMIN", marketA, CompanyType.MARKET)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].batchId", hasItem((int) batchA)))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/batches").with(as(adminB, "ADMIN", marketB, CompanyType.MARKET)))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/v1/batches").param("size", "100").with(as(1L, "PLATFORM_ADMIN", null, null)))
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThan(1)));

        mockMvc.perform(get("/api/v1/reports").with(as(adminA, "ADMIN", marketA, CompanyType.MARKET)))
                .andExpect(jsonPath("$.items[*].reportId", hasItem((int) reportA)));
        mockMvc.perform(get("/api/v1/reports").with(as(adminB, "ADMIN", marketB, CompanyType.MARKET)))
                .andExpect(jsonPath("$.items[*].reportId", not(hasItem((int) reportA))));
        mockMvc.perform(get("/api/v1/reports").param("reportId", String.valueOf(reportA))
                        .with(as(adminB, "ADMIN", marketB, CompanyType.MARKET)))
                .andExpect(status().isForbidden());
    }

    @Test
    void usersAndAuditAreScopedToTheCompany() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("limit", "100").with(as(auditorA, "AUDITOR", marketA, CompanyType.MARKET)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].companyId", everyItem(is((int) marketA))))
                .andExpect(jsonPath("$.items[*].userId", hasItem((int) adminA)))
                .andExpect(jsonPath("$.items[*].userId", not(hasItem((int) adminB))));
        mockMvc.perform(post("/api/v1/users").with(as(auditorA, "AUDITOR", marketA, CompanyType.MARKET))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"email\":\"x@isolamento.test\",\"password\":\"segredo\",\"role\":\"AUDITOR\"}"))
                .andExpect(status().isForbidden());

        // A ação é do admin de A: as triggers gravam o ator de @app_user_id.
        jdbcTemplate.execute("SET @app_user_id = " + adminA);
        jdbcTemplate.update("UPDATE company SET phone = '1130000000' WHERE id = ?", marketA);
        jdbcTemplate.execute("SET @app_user_id = NULL");
        String today = LocalDate.now().toString();
        mockMvc.perform(get("/api/v1/audit-logs").param("from", today).param("to", today)
                        .with(as(adminA, "ADMIN", marketA, CompanyType.MARKET)))
                .andExpect(jsonPath("$.items[*].userId", everyItem(is((int) adminA))))
                .andExpect(jsonPath("$.items[*].affectedEntityId", hasItem((int) marketA)));
        mockMvc.perform(get("/api/v1/audit-logs").param("from", today).param("to", today)
                        .with(as(adminB, "ADMIN", marketB, CompanyType.MARKET)))
                .andExpect(jsonPath("$.items[*].userId", not(hasItem((int) adminA))));
    }

    @Test
    void supplierCatalogMarksWorkedWithPerMarketAndIsClosedToSupplierCompanies() throws Exception {
        String workedWith = "$[?(@.supplierId == " + supplier + ")].workedWith";
        mockMvc.perform(get("/api/v1/suppliers").with(as(adminA, "ADMIN", marketA, CompanyType.MARKET)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(workedWith, hasItem(true)));
        mockMvc.perform(get("/api/v1/suppliers").with(as(adminB, "ADMIN", marketB, CompanyType.MARKET)))
                .andExpect(jsonPath(workedWith, hasItem(false)));
        mockMvc.perform(get("/api/v1/suppliers").param("workedWith", "true").with(as(adminB, "ADMIN", marketB, CompanyType.MARKET)))
                .andExpect(jsonPath("$[*].supplierId", not(hasItem((int) supplier))));

        mockMvc.perform(get("/api/v1/suppliers").with(as(supplierUser, "SUPPLIER", supplier, CompanyType.SUPPLIER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/suppliers").param("supplierId", String.valueOf(supplier))
                        .with(as(supplierUser, "SUPPLIER", supplier, CompanyType.SUPPLIER)))
                .andExpect(status().isOk());
    }

    @Test
    void dashboardCountsSuppliersForTheMarketAndPendingBatchesForTheSupplier() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary").with(as(adminA, "ADMIN", marketA, CompanyType.MARKET)))
                .andExpect(jsonPath("$.suppliers").value(1))
                .andExpect(jsonPath("$.pendingBatches").value(nullValue()))
                .andExpect(jsonPath("$.recentBatches[*].batchId", everyItem(is((int) batchA))));
        mockMvc.perform(get("/api/v1/dashboard/summary").with(as(adminB, "ADMIN", marketB, CompanyType.MARKET)))
                .andExpect(jsonPath("$.suppliers").value(0));
        mockMvc.perform(get("/api/v1/dashboard/summary").with(as(supplierUser, "SUPPLIER", supplier, CompanyType.SUPPLIER)))
                .andExpect(jsonPath("$.suppliers").value(nullValue()))
                .andExpect(jsonPath("$.pendingBatches").value(0));
    }

    private long company(String name, String cnpj, long address, String type) {
        return insert(jdbcTemplate, "INSERT INTO company (name, cnpj, address_id, email, type) VALUES (?, ?, ?, ?, ?)",
                name, cnpj, address, cnpj + "@isolamento.test", type);
    }

    private long user(String email, String role, long companyId) {
        return insert(jdbcTemplate, "INSERT INTO users (name, email, password, role, company_id) VALUES (?, ?, 'x', ?, ?)",
                email, email, role, companyId);
    }

    private RequestPostProcessor as(long userId, String role, Long companyId, CompanyType type) {
        return authentication(new UsernamePasswordAuthenticationToken(
                new CustomUserPrincipal(userId, "actor@isolamento.test", role, companyId, type), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
