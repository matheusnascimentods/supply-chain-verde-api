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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static br.com.anhembi.supplychainverde.JdbcInserts.SEED_MARKET;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RN-17 a RN-20 (010) e 017: o mercado pede ou cancela o lote, e o fornecedor executa (PRODUCTION ativa) ou recusa (final). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BatchLifecycleIT {
    // Fornecedor 3 do seed: o usuário dele virou o ADMIN da companhia na V8.
    private static final long SUPPLIER = 3L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long marketId;
    private long marketUser;
    private long supplierUser;
    private long productId;

    @BeforeEach
    void setUp() {
        marketId = jdbcTemplate.queryForObject("SELECT " + SEED_MARKET, Long.class);
        marketUser = jdbcTemplate.queryForObject("SELECT MIN(id) FROM users WHERE role = 'MANAGER' AND company_id = ?",
                Long.class, marketId);
        supplierUser = jdbcTemplate.queryForObject("SELECT MIN(id) FROM users WHERE company_id = ?", Long.class, SUPPLIER);
        productId = jdbcTemplate.queryForObject("SELECT MIN(id) FROM product WHERE supplier_id = ?", Long.class, SUPPLIER);
    }

    @Test
    void marketRequestsAPendingBatchOnlyWithAProductOfTheSupplier() throws Exception {
        requestBatch(productId, SUPPLIER)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.marketName").value("Mercado Bom Preço"))
                .andExpect(jsonPath("$.marketId").value(marketId))
                .andExpect(jsonPath("$.producedAt").value(nullValue()));

        long otherSupplierProduct = jdbcTemplate.queryForObject(
                "SELECT MIN(id) FROM product WHERE supplier_id <> ?", Long.class, SUPPLIER);
        requestBatch(otherSupplierProduct, SUPPLIER).andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/batches").with(asSupplier()).contentType(MediaType.APPLICATION_JSON)
                        .content(batchBody(productId, SUPPLIER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/batches").param("status", "PENDING").param("size", "100").with(asMarket()))
                .andExpect(jsonPath("$.content[*].status", everyItem(is("PENDING"))));
    }

    @Test
    void supplierRejectsWithAReasonAndTheRejectedBatchIsFinal() throws Exception {
        long batchId = pendingBatch();

        reject(batchId, asMarket()).andExpect(status().isForbidden());
        reject(batchId, asSupplier())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Sem capacidade"));

        reject(batchId, asSupplier()).andExpect(status().isConflict());
        stage(batchId, supplierUser, asSupplier()).andExpect(status().isConflict());
    }

    @Test
    void productionBySupplierActivatesThePendingBatchAndTheMarketCannotStartIt() throws Exception {
        long batchId = pendingBatch();

        stage(batchId, marketUser, asMarket()).andExpect(status().isForbidden());
        stage(batchId, supplierUser, asSupplier()).andExpect(status().isCreated());

        // A listagem é JPQL: o Hibernate faz flush antes, e o lote já aparece ACTIVE.
        mockMvc.perform(get("/api/v1/batches").param("status", "ACTIVE").param("size", "100").with(asSupplier()))
                .andExpect(jsonPath("$.content[?(@.batchId == " + batchId + ")].producedAt", hasItem(LocalDate.now().toString())));
        reject(batchId, asSupplier()).andExpect(status().isConflict());
    }

    @Test
    void responsibleUserMustBelongToTheBatchCompanies() throws Exception {
        long batchId = pendingBatch();
        long outsider = jdbcTemplate.queryForObject(
                "SELECT MIN(id) FROM users WHERE company_id NOT IN (?, ?)", Long.class, marketId, SUPPLIER);

        stage(batchId, outsider, asSupplier()).andExpect(status().isForbidden());
    }

    @Test
    void marketCancelsAPendingBatchAndTheSupplierCannot() throws Exception {
        long batchId = pendingBatch();

        cancel(batchId, asSupplier()).andExpect(status().isForbidden());
        cancel(batchId, asMarket())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        cancel(batchId, asMarket()).andExpect(status().isConflict());
        stage(batchId, supplierUser, asSupplier()).andExpect(status().isConflict());
    }

    @Test
    void platformAdminRejectsAndCancelsButStillFollowsTheTransitions() throws Exception {
        long rejected = pendingBatch();
        reject(rejected, asPlatformAdmin()).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        cancel(rejected, asPlatformAdmin()).andExpect(status().isConflict());

        cancel(pendingBatch(), asPlatformAdmin()).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void supplierRoleCalculatesTheEmissionOfItsStage() throws Exception {
        long batchId = pendingBatch();
        String body = stage(batchId, supplierUser, asSupplier()).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long chainId = ((Number) JsonPath.read(body, "$.chainId")).longValue();

        mockMvc.perform(post("/api/v1/stages/{id}/emission", chainId)
                        .with(as(TestPrincipals.of(supplierUser, "supplier@example.com", "SUPPLIER", SUPPLIER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calculationMethod\":\"GHG_PROTOCOL\",\"energySource\":\"ELECTRICITY\",\"consumption\":100}"))
                .andExpect(status().isCreated());
    }

    @Test
    void removedCalculationMethodIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/stages/{id}/emission", 1L)
                        .with(as(TestPrincipals.of(supplierUser, "supplier@example.com", "SUPPLIER", SUPPLIER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chainId\":1,\"calculationMethod\":\"IPCC\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo da requisição inválido."));
    }

    @Test
    void productionStartsAtTheSupplierAndEndsAtTheInformedDestination() throws Exception {
        String supplierCity = jdbcTemplate.queryForObject(
                "SELECT a.city FROM company c JOIN address a ON a.id = c.address_id WHERE c.id = ?", String.class, SUPPLIER);

        stage(pendingBatch(), supplierUser, asSupplier()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.originAddress.city").value(supplierCity))
                .andExpect(jsonPath("$.destinationAddress.city").value("Campinas"))
                .andExpect(jsonPath("$.destinationAddress.addressId").isNumber());
    }

    @Test
    void stageWithoutDestinationIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/batches/{id}/stages", pendingBatch()).with(asSupplier())
                        .header("X-Responsible-User-Id", supplierUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageType\":\"PRODUCTION\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Destino da etapa é obrigatório."));
    }

    private long pendingBatch() throws Exception {
        String body = requestBatch(productId, SUPPLIER).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.batchId")).longValue();
    }

    private ResultActions requestBatch(long product, long supplier) throws Exception {
        return mockMvc.perform(post("/api/v1/batches").with(asMarket())
                .contentType(MediaType.APPLICATION_JSON).content(batchBody(product, supplier)));
    }

    private String batchBody(long product, long supplier) {
        return "{\"productId\":" + product + ",\"supplierId\":" + supplier + ",\"quantity\":10}";
    }

    private ResultActions reject(long batchId, RequestPostProcessor actor) throws Exception {
        return mockMvc.perform(patch("/api/v1/batches/{id}/reject", batchId).with(actor)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Sem capacidade\"}"));
    }

    private ResultActions cancel(long batchId, RequestPostProcessor actor) throws Exception {
        return mockMvc.perform(patch("/api/v1/batches/{id}/cancel", batchId).with(actor));
    }

    private ResultActions stage(long batchId, long responsibleUser, RequestPostProcessor actor) throws Exception {
        return mockMvc.perform(post("/api/v1/batches/{id}/stages", batchId).with(actor)
                .header("X-Responsible-User-Id", responsibleUser)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"stageType\":\"PRODUCTION\",\"destination\":{\"street\":\"Rua A\",\"number\":\"1\",\"neighborhood\":\"Centro\",\"zipCode\":\"13000000\",\"city\":\"Campinas\",\"state\":\"SP\"}}"));
    }

    private RequestPostProcessor asMarket() {
        return as(TestPrincipals.market(marketUser, "MANAGER", marketId));
    }

    private RequestPostProcessor asSupplier() {
        return as(TestPrincipals.of(supplierUser, "supplier@example.com", "ADMIN", SUPPLIER));
    }

    private RequestPostProcessor asPlatformAdmin() {
        long admin = jdbcTemplate.queryForObject("SELECT MIN(id) FROM users WHERE role = 'PLATFORM_ADMIN'", Long.class);
        return as(TestPrincipals.of(admin, "platform@example.com", "PLATFORM_ADMIN", null));
    }

    private RequestPostProcessor as(CustomUserPrincipal principal) {
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()))));
    }
}
