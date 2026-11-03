package br.com.anhembi.supplychainverde.infrastructure.persistence;

import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static br.com.anhembi.supplychainverde.JdbcInserts.DEMO_MARKET;
import static org.assertj.core.api.Assertions.assertThat;

/** Estado dos seeds (criados até 2026-10-29) depois da V9 (RN-24 a RN-28). Companhias de outros ITs têm id maior que o mercado. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigrationToCompanyIT {
    private static final String SEEDED = " c.id <= " + DEMO_MARKET;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void suppliersBecameSupplierCompaniesWithTheSameId() {
        assertThat(count("SELECT COUNT(*) FROM company c WHERE c.type = 'SUPPLIER' AND" + SEEDED)).isEqualTo(41);
        assertThat(jdbcTemplate.queryForObject("SELECT name FROM company WHERE id = 3", String.class))
                .isEqualTo("Beneficiadora Circular de Grãos");
        // Sem usuário vinculado, o email é o placeholder; com usuário, o do usuário.
        assertThat(jdbcTemplate.queryForObject("SELECT email FROM company WHERE id = 1", String.class))
                .isEqualTo("fornecedor1@supplychainverde.local");
        assertThat(jdbcTemplate.queryForObject("SELECT email FROM company WHERE id = 3", String.class))
                .isEqualTo("d.upamecano@bayern.com");
    }

    @Test
    void everyCompanyHasOneActivePlanAndTheMarketIsProMax() {
        assertThat(count("""
                SELECT COUNT(*) FROM company c
                WHERE (SELECT COUNT(*) FROM company_plan cp WHERE cp.company_id = c.id AND cp.ended_at IS NULL) <> 1
                  AND""" + SEEDED)).isZero();
        assertThat(jdbcTemplate.queryForList("""
                SELECT DISTINCT CONCAT(c.type, ':', p.code) FROM company c
                JOIN company_plan cp ON cp.company_id = c.id JOIN plan p ON p.id = cp.plan_id
                WHERE""" + SEEDED, String.class))
                .containsExactlyInAnyOrder("SUPPLIER:BASIC", "MARKET:PRO_MAX");
        assertThat(count("SELECT COUNT(*) FROM company c WHERE c.type = 'MARKET' AND" + SEEDED)).isEqualTo(1);
    }

    @Test
    void usersMovedToTheirCompaniesWithOnePlatformAdmin() {
        assertThat(jdbcTemplate.queryForList("SELECT email FROM users WHERE role = 'PLATFORM_ADMIN'", String.class))
                .containsExactly("m.neuer@bayern.com");
        // Os 6 usuários SUPPLIER do seed viraram ADMIN do próprio fornecedor.
        assertThat(count("SELECT COUNT(*) FROM users WHERE role = 'SUPPLIER'")).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT CONCAT(role, ':', company_id) FROM users WHERE email = 'd.upamecano@bayern.com'", String.class))
                .isEqualTo("ADMIN:3");
        assertThat(count("SELECT COUNT(*) FROM users WHERE created_at < '2026-10-30' AND role IN ('MANAGER', 'AUDITOR') AND company_id <> "
                + DEMO_MARKET)).isZero();
        // RN-08 nos dados migrados: toda companhia com usuários tem um ADMIN. Fornecedor sem usuário continua sem.
        assertThat(count("""
                SELECT COUNT(*) FROM company c
                WHERE EXISTS (SELECT 1 FROM users u WHERE u.company_id = c.id)
                  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.company_id = c.id AND u.role = 'ADMIN')
                  AND""" + SEEDED)).isZero();
    }

    @Test
    void batchesAndReportsBelongToTheMarketAndProductsToTheirSupplier() {
        assertThat(count("SELECT COUNT(*) FROM batch b JOIN product p ON p.id = b.product_id WHERE p.supplier_id <> b.supplier_id"))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM batch WHERE created_at < '2026-10-30' AND (status <> 'ACTIVE' OR market_id <> " + DEMO_MARKET + ")"))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM report WHERE created_at < '2026-10-30' AND market_id <> " + DEMO_MARKET)).isZero();
        // RN-28: os 52 produtos do seed continuam existindo, com cópias para os fornecedores extras.
        assertThat(count("SELECT COUNT(*) FROM product WHERE id <= 52")).isEqualTo(52);
    }

    private long count(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
}
