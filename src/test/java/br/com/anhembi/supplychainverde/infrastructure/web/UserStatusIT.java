package br.com.anhembi.supplychainverde.infrastructure.web;

import br.com.anhembi.supplychainverde.TestPrincipals;
import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.service.EmailSender;
import br.com.anhembi.supplychainverde.domain.service.PasswordHasher;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import br.com.anhembi.supplychainverde.infrastructure.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static br.com.anhembi.supplychainverde.JdbcInserts.insert;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Feature 016, RN-02 e RN-05 a RN-14, num mercado com teto de 3 assentos e um único ADMIN. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserStatusIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private EmailSender emailSender;

    private long companyId;
    private long adminId;
    private long managerId;

    @BeforeEach
    void setUp() {
        long address = insert(jdbcTemplate, """
                INSERT INTO address (street, number, neighborhood, zip_code, city, state)
                VALUES ('Rua do Assento', '1', 'Centro', '01000000', 'São Paulo', 'SP')
                """);
        companyId = insert(jdbcTemplate, """
                INSERT INTO company (name, cnpj, address_id, email, type, max_users)
                VALUES ('Mercado Assento', '98765432000198', ?, 'assento@it.test', 'MARKET', 3)
                """, address);
        adminId = user("admin@assento.test", "ADMIN");
        managerId = user("gestor@assento.test", "MANAGER");
    }

    @Test
    void deactivatingFreesASeatAndReactivatingNeedsOne() throws Exception {
        long auditorId = user("auditor@assento.test", "AUDITOR");
        long inactiveId = user("inativo@assento.test", "AUDITOR");
        jdbcTemplate.update("UPDATE users SET is_active = FALSE WHERE id = ?", inactiveId);

        // 3 ativos de 3: o inativo não volta.
        setActive(inactiveId, true).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Sem assentos livres: 3 de 3 em uso."));

        setActive(auditorId, false).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        setActive(inactiveId, true).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void aPendingInviteHoldsTheLastSeat() throws Exception {
        long inactiveId = user("inativo@assento.test", "AUDITOR");
        jdbcTemplate.update("UPDATE users SET is_active = FALSE WHERE id = ?", inactiveId);
        mockMvc.perform(post("/api/v1/invites").with(as(adminId, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"novo@assento.test\",\"role\":\"AUDITOR\"}"))
                .andExpect(status().isCreated());

        setActive(inactiveId, true).andExpect(status().isConflict());
    }

    @Test
    void sameStatusIsANoOp() throws Exception {
        setActive(managerId, true).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void nobodyDeactivatesThemselvesNorTheLastActiveAdmin() throws Exception {
        setActive(adminId, false).andExpect(status().isConflict());

        // Com dois ADMIN, um desativa o outro; o que sobra é o último ativo e fica protegido.
        long otherAdminId = user("admin2@assento.test", "ADMIN");
        setActive(adminId, false, as(otherAdminId, "ADMIN")).andExpect(status().isOk());
        setActive(otherAdminId, false, asPlatformAdmin()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A companhia precisa de pelo menos um ADMIN ativo."));
    }

    @Test
    void scopeIsTheSameAsTheRoleChange() throws Exception {
        setActive(managerId, false, TestPrincipals.market(adminId, "ADMIN", companyId + 1000)).andExpect(status().isForbidden());
        setActive(managerId, false, as(managerId, "MANAGER")).andExpect(status().isForbidden());
        setActive(managerId, false, asPlatformAdmin()).andExpect(status().isOk());
        long platformAdminId = jdbcTemplate.queryForObject("SELECT MIN(id) FROM users WHERE role = 'PLATFORM_ADMIN'", Long.class);
        setActive(platformAdminId, false, asPlatformAdmin()).andExpect(status().isBadRequest());
        setActive(999_999L, false).andExpect(status().isNotFound());
    }

    @Test
    void theTokenOfADeactivatedUserStopsWorkingAndTheLoginIsRefused() throws Exception {
        jdbcTemplate.update("UPDATE users SET password = ? WHERE id = ?", passwordHasher.hash("segredo123"), managerId);
        String bearer = "Bearer " + jwtTokenProvider.generateToken(
                User.builder().userId(managerId).email("gestor@assento.test").role(UserRole.MANAGER).companyId(companyId).build(),
                CompanyType.MARKET);
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", bearer)).andExpect(status().isOk());

        setActive(managerId, false).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", bearer)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"gestor@assento.test\",\"password\":\"segredo123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Usuário desativado."));
    }

    @Test
    void anInactiveUserKeepsTheRoleAndTheEmail() throws Exception {
        setActive(managerId, false).andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/users/{id}/role", managerId).with(as(adminId, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"AUDITOR\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/invites").with(as(adminId, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"gestor@assento.test\",\"role\":\"AUDITOR\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email pertence a um usuário desativado. Reative-o na lista de usuários."));
    }

    @Test
    void theListShowsTheStatus() throws Exception {
        setActive(managerId, false).andExpect(status().isOk());

        // Sem filtro de email: o índice FULLTEXT só enxerga linhas commitadas, e o teste roda numa transação.
        mockMvc.perform(get("/api/v1/users").with(as(adminId, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.userId == " + managerId + ")].active").value(false))
                .andExpect(jsonPath("$.items[?(@.userId == " + adminId + ")].active").value(true));
    }

    @Test
    void deactivationIsAudited() throws Exception {
        setActive(managerId, false).andExpect(status().isOk());
        entityManager.flush();

        List<String> after = jdbcTemplate.queryForList("""
                SELECT JSON_EXTRACT(after_data, '$.is_active') FROM audit_log
                WHERE affected_table = 'users' AND affected_entity_id = ? AND action = 'UPDATE'
                """, String.class, managerId);
        assertThat(after).containsExactly("0");
    }

    private long user(String email, String role) {
        return insert(jdbcTemplate, "INSERT INTO users (name, email, password, role, company_id) VALUES ('U', ?, 'x', ?, ?)",
                email, role, companyId);
    }

    private ResultActions setActive(long userId, boolean active) throws Exception {
        return setActive(userId, active, as(adminId, "ADMIN"));
    }

    private ResultActions setActive(long userId, boolean active, CustomUserPrincipal principal) throws Exception {
        return setActive(userId, active, authenticate(principal));
    }

    private ResultActions setActive(long userId, boolean active, RequestPostProcessor actor) throws Exception {
        return mockMvc.perform(patch("/api/v1/users/{id}/status", userId).with(actor)
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":" + active + "}"));
    }

    private RequestPostProcessor as(long userId, String role) {
        return authenticate(TestPrincipals.market(userId, role, companyId));
    }

    private RequestPostProcessor asPlatformAdmin() {
        return authenticate(new CustomUserPrincipal(1L, "platform@example.com", "PLATFORM_ADMIN", null, null));
    }

    private RequestPostProcessor authenticate(CustomUserPrincipal principal) {
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()))));
    }
}
