package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF-01 a RF-03: a companhia se cadastra sozinha e o token devolvido já funciona. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CompanySignupIT {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsPlansWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].code").value("BASIC"))
                .andExpect(jsonPath("$[0].maxUsers").value(3));
    }

    @Test
    void marketSignsUpAndUsesTheApiWithTheReturnedToken() throws Exception {
        String body = mockMvc.perform(signup("MARKET", "PRO", "98765432000198", "ana@bompreco.test"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.companyType").value("MARKET"))
                .andExpect(jsonPath("$.supplierId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String bearer = "Bearer " + JsonPath.read(body, "$.token");
        Number companyId = JsonPath.read(body, "$.companyId");

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@bompreco.test"));
        mockMvc.perform(get("/api/v1/companies/me").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId").value(companyId.longValue()))
                .andExpect(jsonPath("$.type").value("MARKET"))
                .andExpect(jsonPath("$.cnpj").value("98765432000198"))
                .andExpect(jsonPath("$.address.city").value("São Paulo"))
                .andExpect(jsonPath("$.plan.code").value("PRO"));
        mockMvc.perform(patch("/api/v1/companies/me").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Mercado Bom Preço S.A.\", \"phone\": \"1133334444\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mercado Bom Preço S.A."))
                .andExpect(jsonPath("$.phone").value("1133334444"))
                .andExpect(jsonPath("$.email").value("contato@empresa.test"));
    }

    @Test
    void supplierTokenCarriesTheCompanyAsSupplierId() throws Exception {
        String body = mockMvc.perform(signup("SUPPLIER", "BASIC", "98765432000279", "joao@fornecedor.test"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyType").value("SUPPLIER"))
                .andReturn().getResponse().getContentAsString();
        Number companyId = JsonPath.read(body, "$.companyId");

        mockMvc.perform(get("/api/v1/suppliers").param("supplierId", companyId.toString())
                        .header("Authorization", "Bearer " + JsonPath.read(body, "$.token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplierId").value(companyId.longValue()));
        assertThat(((Number) JsonPath.read(body, "$.supplierId")).longValue()).isEqualTo(companyId.longValue());
    }

    @Test
    void rejectsDuplicatedCnpjAndEmailAndUnknownPlan() throws Exception {
        mockMvc.perform(signup("MARKET", "PRO", "98765432000350", "maria@mercado.test")).andExpect(status().isCreated());

        mockMvc.perform(signup("MARKET", "PRO", "98765432000350", "outra@mercado.test")).andExpect(status().isConflict());
        mockMvc.perform(signup("MARKET", "PRO", "98765432000430", "maria@mercado.test")).andExpect(status().isConflict());
        mockMvc.perform(signup("MARKET", "GOLD", "98765432000430", "nova@mercado.test")).andExpect(status().isBadRequest());
    }

    @Test
    void ownCompanyNeedsAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/companies/me")).andExpect(status().isUnauthorized());
    }

    private RequestBuilder signup(String type, String planCode, String cnpj,
            String adminEmail) {
        return post("/api/v1/companies").contentType(MediaType.APPLICATION_JSON).content("""
                {
                  "planCode": "%s",
                  "type": "%s",
                  "company": {
                    "name": "Empresa Teste",
                    "cnpj": "%s",
                    "email": "contato@empresa.test",
                    "phone": "11999990000",
                    "address": {"street": "Rua A", "number": "10", "neighborhood": "Centro", "complement": null,
                                "zipCode": "01001000", "city": "São Paulo", "state": "SP"}
                  },
                  "admin": {"name": "Admin Teste", "email": "%s", "password": "segredo123"}
                }
                """.formatted(planCode, type, cnpj, adminEmail));
    }
}
