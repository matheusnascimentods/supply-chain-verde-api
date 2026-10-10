package br.com.anhembi.supplychainverde.infrastructure.web;

import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class SupplierRecommendationIT {
    // Todos os fornecedores do teste têm este termo no nome; a busca isola-os do seed das migrations.
    private static final String SEARCH = "Zpkrecomenda";
    private static final BigDecimal TOLERANCE = new BigDecimal("0.0001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long addressId;
    private long userId;

    @BeforeEach
    void setUp() {
        addressId = jdbcTemplate.queryForObject("""
                INSERT INTO address (street, number, neighborhood, zip_code, city, state)
                VALUES ('Rua Teste', '1', 'Centro', '01000000', 'São Paulo', 'SP')
                RETURNING address_id
                """, Long.class);
        userId = jdbcTemplate.queryForObject("SELECT MIN(user_id) FROM users", Long.class);
    }

    @Test
    void ordersSuppliersOfProductByCo2PerUnitWithoutHistoryLast() throws Exception {
        long product = product("AGRICULTURE", "KG");
        long clean = supplier("Alfa", "98765432000198");
        long dirty = supplier("Beta", "98765432000279");
        long withoutHistory = supplier("Gama", "98765432000350");
        batch(clean, product, "100", "40");
        batch(dirty, product, "100", "60");

        mockMvc.perform(get("/api/v1/suppliers")
                        .param("ranked", "true")
                        .param("productId", String.valueOf(product))
                        .param("search", SEARCH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[0].supplierId").value(clean))
                .andExpect(jsonPath("$.items[0].co2KgPerUnit").value(closeTo(new BigDecimal("0.4"), TOLERANCE)))
                .andExpect(jsonPath("$.items[1].supplierId").value(dirty))
                .andExpect(jsonPath("$.items[1].co2KgPerUnit").value(closeTo(new BigDecimal("0.6"), TOLERANCE)))
                .andExpect(jsonPath("$.items[2].supplierId").value(withoutHistory))
                .andExpect(jsonPath("$.items[2].co2KgPerUnit").value(nullValue()));
    }

    @Test
    void comparesCategoryByWeightedAverageAndIgnoresOtherUnits() throws Exception {
        long kgProduct = product("FORESTRY", "KG");
        long otherKgProduct = product("FORESTRY", "KG");
        long tonProduct = product("FORESTRY", "TON");
        long mixed = supplier("Delta", "98765432000430");
        long steady = supplier("Epsilon", "98765432000511");
        long tonOnly = supplier("Zeta", "98765432000600");
        // Média das médias de Delta seria (0,1 + 0,8) / 2 = 0,45; a ponderada é 801 / 1010 ≈ 0,7931.
        batch(mixed, kgProduct, "10", "1");
        batch(mixed, otherKgProduct, "1000", "800");
        batch(steady, kgProduct, "100", "50");
        batch(tonOnly, tonProduct, "1", "0.0001");

        mockMvc.perform(get("/api/v1/suppliers")
                        .param("ranked", "true")
                        .param("category", "FORESTRY")
                        .param("unit", "KG")
                        .param("search", SEARCH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].supplierId").value(steady))
                .andExpect(jsonPath("$.items[0].co2KgPerUnit").value(closeTo(new BigDecimal("0.5"), TOLERANCE)))
                .andExpect(jsonPath("$.items[1].supplierId").value(mixed))
                .andExpect(jsonPath("$.items[1].co2KgPerUnit").value(closeTo(new BigDecimal("0.7931"), TOLERANCE)))
                .andExpect(jsonPath("$.items[2].supplierId").value(tonOnly))
                .andExpect(jsonPath("$.items[2].co2KgPerUnit").value(nullValue()));
    }

    @Test
    void keepsPlainRankingWithoutCo2PerUnit() throws Exception {
        supplier("Eta", "98765432000198");

        mockMvc.perform(get("/api/v1/suppliers").param("ranked", "true").param("search", SEARCH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].co2KgPerUnit").value(nullValue()));
    }

    @Test
    void rejectsInvalidParameterCombinations() throws Exception {
        mockMvc.perform(get("/api/v1/suppliers").param("ranked", "true").param("productId", "1")
                        .param("category", "AGRICULTURE").param("unit", "KG"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/suppliers").param("ranked", "true").param("category", "AGRICULTURE"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/suppliers").param("category", "AGRICULTURE").param("unit", "KG"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/suppliers").param("ranked", "true").param("category", "XYZ").param("unit", "KG"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForUnknownProduct() throws Exception {
        mockMvc.perform(get("/api/v1/suppliers").param("ranked", "true").param("productId", "999999999"))
                .andExpect(status().isNotFound());
    }

    private long product(String category, String unit) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO product (name, category, unit) VALUES ('Produto teste', ?::product_category, ?::product_unit) RETURNING product_id",
                Long.class, category, unit);
    }

    private long supplier(String name, String cnpj) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO supplier (name, cnpj, address_id) VALUES (?, ?, ?) RETURNING supplier_id",
                Long.class, SEARCH + " " + name, cnpj, addressId);
    }

    private void batch(long supplierId, long productId, String quantity, String co2Kg) {
        long batchId = jdbcTemplate.queryForObject(
                "INSERT INTO batch (product_id, supplier_id, quantity, produced_at) VALUES (?, ?, ?::numeric, CURRENT_DATE) RETURNING batch_id",
                Long.class, productId, supplierId, quantity);
        long chainId = jdbcTemplate.queryForObject(
                "INSERT INTO chain (batch_id, responsible_user_id, stage_type, started_at) VALUES (?, ?, 'PRODUCTION', NOW()) RETURNING chain_id",
                Long.class, batchId, userId);
        jdbcTemplate.update(
                "INSERT INTO carbon_emission (chain_id, emission_factor, co2_kg, calculation_method) VALUES (?, 1, ?::numeric, 'IPCC')",
                chainId, co2Kg);
    }
}
