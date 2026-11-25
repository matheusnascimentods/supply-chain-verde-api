package br.com.anhembi.supplychainverde.infrastructure.web;

import br.com.anhembi.supplychainverde.TestPrincipals;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static br.com.anhembi.supplychainverde.JdbcInserts.insert;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RN-16 e RF-07: o produto é do fornecedor; o mercado só lê o catálogo e filtra por fornecedor. 015: ordem, busca e filtros do catálogo. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductControllerIT {
    private static final String PRODUCT = """
            {"name": "Café sustentável", "category": "AGRICULTURE", "unit": "KG", "description": "Lote de teste"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void supplierCreatesItsOwnProductAndSeesOnlyItsProducts() throws Exception {
        String body = mockMvc.perform(post("/api/v1/products").with(as(TestPrincipals.of(3L, "s@example.com", "SUPPLIER", 1L)))
                        .contentType(APPLICATION_JSON).content(PRODUCT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.supplierId").value(1))
                .andExpect(jsonPath("$.name").value("Café sustentável"))
                .andReturn().getResponse().getContentAsString();
        int productId = JsonPath.read(body, "$.productId");

        mockMvc.perform(get("/api/v1/products").param("limit", "100").with(as(TestPrincipals.of(3L, "s@example.com", "SUPPLIER", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].supplierId", everyItem(is(1))))
                .andExpect(jsonPath("$.items[*].productId", hasItem(productId)))
                // RN-07: sem os campos do catálogo.
                .andExpect(jsonPath("$.items[*].supplierName", everyItem(nullValue())))
                .andExpect(jsonPath("$.items[*].co2KgPerUnit", everyItem(nullValue())))
                .andExpect(jsonPath("$.items[*].acceptedBatchesLast30Days", everyItem(nullValue())));
        mockMvc.perform(put("/api/v1/products/{id}", productId).with(as(TestPrincipals.of(9L, "o@example.com", "SUPPLIER", 2L)))
                        .contentType(APPLICATION_JSON).content(PRODUCT))
                .andExpect(status().isForbidden());
    }

    @Test
    void marketReadsTheCatalogFilteredBySupplierButDoesNotCreateProducts() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("supplierId", "2").param("limit", "100")
                        .with(as(TestPrincipals.market(4L, "MANAGER", 42L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].supplierId", everyItem(is(2))));
        mockMvc.perform(post("/api/v1/products").with(as(TestPrincipals.market(4L, "ADMIN", 42L)))
                        .contentType(APPLICATION_JSON).content(PRODUCT))
                .andExpect(status().isForbidden());
    }

    // RN-02/RN-03/RN-04: só os lotes ACTIVE do mercado nos 30 dias contam; depois CO2 por unidade, null no fim.
    @Test
    void marketCatalogOrdersByRecentAcceptedBatchesThenCo2PerUnit() throws Exception {
        long market = market("98765432000101");
        long otherMarket = market("98765432000292");
        long supplier = insert(jdbcTemplate, """
                INSERT INTO company (name, cnpj, address_id, email, type)
                VALUES ('Fornecedor Catálogo', '98765432000373', ?, 'catalogo@fornecedor.test', 'SUPPLIER')
                """, addressId());
        long ordered = product(supplier, "Delta", "AGRICULTURE", "KG");
        long cheap = product(supplier, "Beta", "TEXTILE", "KG");
        long expensive = product(supplier, "Alfa", "FORESTRY", "KG");
        long noHistory = product(supplier, "Aaa", "OTHER", "UNIT");
        batch(ordered, supplier, market, "ACTIVE", 0, null);
        batch(ordered, supplier, market, "ACTIVE", 29, null);
        batch(cheap, supplier, market, "ACTIVE", 31, "10");
        batch(cheap, supplier, market, "REJECTED", 0, null);
        batch(expensive, supplier, otherMarket, "ACTIVE", 0, "50");
        batch(expensive, supplier, market, "PENDING", 0, null);

        mockMvc.perform(get("/api/v1/products").param("supplierId", String.valueOf(supplier))
                        .with(as(TestPrincipals.market(4L, "MANAGER", market))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].productId").value(is(ids(ordered, cheap, expensive, noHistory))))
                .andExpect(jsonPath("$.items[*].acceptedBatchesLast30Days").value(is(List.of(2, 0, 0, 0))))
                .andExpect(jsonPath("$.items[0].co2KgPerUnit").value(nullValue()))
                .andExpect(jsonPath("$.items[1].co2KgPerUnit").value(0.1))
                .andExpect(jsonPath("$.items[2].co2KgPerUnit").value(0.5))
                .andExpect(jsonPath("$.items[3].co2KgPerUnit").value(nullValue()))
                .andExpect(jsonPath("$.items[*].supplierName", everyItem(is("Fornecedor Catálogo"))))
                .andExpect(jsonPath("$.totalPages").value(1));
        // PLATFORM_ADMIN conta os lotes de todos os mercados.
        mockMvc.perform(get("/api/v1/products").param("supplierId", String.valueOf(supplier))
                        .with(as(TestPrincipals.of(1L, "admin@example.com", "PLATFORM_ADMIN", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].productId").value(is(ids(ordered, expensive, cheap, noHistory))));

        // RN-06: category e unit filtram sozinhos e juntos.
        assertCatalog(supplier, market, "FORESTRY", null, expensive);
        assertCatalog(supplier, market, null, "UNIT", noHistory);
        assertCatalog(supplier, market, "AGRICULTURE", "KG", ordered);
        assertCatalog(supplier, market, "AGRICULTURE", "TON");
    }

    // RN-05 sobre o seed (o FULLTEXT só vê linhas commitadas).
    @Test
    void marketCatalogSearchesProductSupplierAndCategoryLabel() throws Exception {
        RequestPostProcessor market = as(TestPrincipals.market(4L, "MANAGER", 42L));
        mockMvc.perform(get("/api/v1/products").param("search", "serra").param("limit", "100").with(market))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].supplierName", hasItem("Grãos da Serra Cooperativa")));
        mockMvc.perform(get("/api/v1/products").param("search", "cafe").param("limit", "100").with(market))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].name", hasItems("Café Arábica Orgânico", "Café Bourbon Amarelo de Montanha",
                        "Café Solúvel de Origem Certificada")));
        mockMvc.perform(get("/api/v1/products").param("search", "Têxtil").param("limit", "100").with(market))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].category", hasItem("TEXTILE")));
    }

    @Test
    void shouldRejectUnauthenticatedProductListing() throws Exception {
        mockMvc.perform(get("/api/v1/products")).andExpect(status().isUnauthorized());
    }

    private void assertCatalog(long supplier, long market, String category, String unit, Long... productIds) throws Exception {
        var request = get("/api/v1/products").param("supplierId", String.valueOf(supplier))
                .with(as(TestPrincipals.market(4L, "MANAGER", market)));
        if (category != null) {
            request.param("category", category);
        }
        if (unit != null) {
            request.param("unit", unit);
        }
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].productId").value(is(ids(productIds))));
    }

    private static List<Integer> ids(Long... productIds) {
        return Arrays.stream(productIds).map(Long::intValue).toList();
    }

    private long addressId() {
        return jdbcTemplate.queryForObject("SELECT MIN(id) FROM address", Long.class);
    }

    private long market(String cnpj) {
        return insert(jdbcTemplate, """
                INSERT INTO company (name, cnpj, address_id, email, type)
                VALUES ('Mercado Catálogo', ?, ?, CONCAT(?, '@mercado.test'), 'MARKET')
                """, cnpj, addressId(), cnpj);
    }

    private long product(long supplierId, String name, String category, String unit) {
        return insert(jdbcTemplate, "INSERT INTO product (supplier_id, name, category, unit) VALUES (?, ?, ?, ?)",
                supplierId, name, category, unit);
    }

    private void batch(long productId, long supplierId, long marketId, String status, int daysAgo, String co2Kg) {
        long batchId = insert(jdbcTemplate, """
                INSERT INTO batch (product_id, supplier_id, market_id, status, rejection_reason, quantity, produced_at, created_at)
                VALUES (?, ?, ?, ?, IF(? = 'REJECTED', 'teste', NULL), 100, CURRENT_DATE, NOW() - INTERVAL ? DAY)
                """, productId, supplierId, marketId, status, status, daysAgo);
        if (co2Kg != null) {
            long chainId = insert(jdbcTemplate, """
                    INSERT INTO chain (batch_id, destination_address_id, responsible_user_id, stage_type, started_at)
                    VALUES (?, (SELECT c.address_id FROM company c JOIN batch b ON b.supplier_id = c.id WHERE b.id = ?), (SELECT MIN(id) FROM users), 'PRODUCTION', NOW())
                    """, batchId, batchId);
            jdbcTemplate.update("INSERT INTO carbon_emission (chain_id, emission_factor, co2_kg, calculation_method) VALUES (?, 1, ?, 'IPCC')",
                    chainId, co2Kg);
        }
    }

    private RequestPostProcessor as(CustomUserPrincipal principal) {
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()))));
    }
}
