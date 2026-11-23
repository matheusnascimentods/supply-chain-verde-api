package br.com.anhembi.supplychainverde.infrastructure.web;

import br.com.anhembi.supplychainverde.TestPrincipals;
import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RN-15 (016) sobre o seed (V10/V11): o Mercado Bom Preço tem 1 ADMIN (h.kane), 4 MANAGER e 5 AUDITOR. Os usuários
 * do seed já estão commitados, então a busca FULLTEXT por email os enxerga dentro da transação do teste.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserListIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long companyId;
    private long adminId;

    @BeforeEach
    void setUp() {
        adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = 'h.kane@bayern.com'", Long.class);
        companyId = jdbcTemplate.queryForObject("SELECT company_id FROM users WHERE id = ?", Long.class, adminId);
    }

    @Test
    void withoutFiltersListsTheWholeCompany() throws Exception {
        mockMvc.perform(list())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(10)));
    }

    @Test
    void repeatedRoleIsAnOr() throws Exception {
        mockMvc.perform(list().param("role", "MANAGER").param("role", "AUDITOR"))
                .andExpect(jsonPath("$.items", hasSize(9)))
                .andExpect(jsonPath("$.items[*].role", everyItem(org.hamcrest.Matchers.oneOf("MANAGER", "AUDITOR"))));
        mockMvc.perform(list().param("role", "ADMIN"))
                .andExpect(jsonPath("$.items[*].email", containsInAnyOrder("h.kane@bayern.com")));
    }

    @Test
    void activeFiltersAndCombinesWithRoleAndEmail() throws Exception {
        jdbcTemplate.update("UPDATE users SET is_active = FALSE WHERE email IN ('j.tah@bayern.com', 'a.davies@bayern.com')");

        mockMvc.perform(list().param("active", "false"))
                .andExpect(jsonPath("$.items[*].email", containsInAnyOrder("j.tah@bayern.com", "a.davies@bayern.com")))
                .andExpect(jsonPath("$.items[*].active", everyItem(is(false))));
        mockMvc.perform(list().param("active", "true"))
                .andExpect(jsonPath("$.items", hasSize(8)));
        mockMvc.perform(list().param("role", "MANAGER").param("active", "false"))
                .andExpect(jsonPath("$.items[*].email", containsInAnyOrder("j.tah@bayern.com")));
        mockMvc.perform(list().param("email", "davies").param("role", "AUDITOR").param("active", "false"))
                .andExpect(jsonPath("$.items[*].email", containsInAnyOrder("a.davies@bayern.com")));
        mockMvc.perform(list().param("email", "davies").param("active", "true"))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void theTotalFollowsTheFilters() throws Exception {
        mockMvc.perform(list().param("role", "AUDITOR").param("limit", "2"))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void anUnknownRoleIsABadRequest() throws Exception {
        mockMvc.perform(list().param("role", "CHEFE")).andExpect(status().isBadRequest());
    }

    private MockHttpServletRequestBuilder list() {
        return get("/api/v1/users").with(asAdmin());
    }

    private RequestPostProcessor asAdmin() {
        CustomUserPrincipal principal = TestPrincipals.market(adminId, "ADMIN", companyId);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
}
