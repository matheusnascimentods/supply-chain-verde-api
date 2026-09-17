package br.com.anhembi.supplychainverde.infrastructure.web;

import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.ProductJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductJpaRepository productRepository;

    @BeforeEach
    void cleanDatabase() {
        productRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateProductThroughControllerAndPersistIt() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Café sustentável",
                                  "category": "AGRICULTURE",
                                  "unit": "KG",
                                  "description": "Lote de teste"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Café sustentável"))
                .andExpect(jsonPath("$.category").value("AGRICULTURE"));

        assertThat(productRepository.findAll()).hasSize(1);

    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldListProductsForAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Arroz",
                                  "category": "AGRICULTURE",
                                  "unit": "KG",
                                  "description": "Integral"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void shouldRejectUnauthenticatedProductListing() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized());
    }
}
