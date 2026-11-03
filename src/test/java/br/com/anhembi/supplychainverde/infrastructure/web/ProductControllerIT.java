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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RN-16 e RF-07: o produto é do fornecedor; o mercado só lê o catálogo e filtra por fornecedor. */
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
                .andExpect(jsonPath("$.items[*].productId", hasItem(productId)));
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

    @Test
    void shouldRejectUnauthenticatedProductListing() throws Exception {
        mockMvc.perform(get("/api/v1/products")).andExpect(status().isUnauthorized());
    }

    private RequestPostProcessor as(CustomUserPrincipal principal) {
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()))));
    }
}
