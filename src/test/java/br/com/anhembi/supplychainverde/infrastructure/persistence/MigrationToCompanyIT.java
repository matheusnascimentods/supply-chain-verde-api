package br.com.anhembi.supplychainverde.infrastructure.persistence;

import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static br.com.anhembi.supplychainverde.JdbcInserts.DEMO_MARKET;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Estado dos seeds (criados até 2026-10-29) depois da V9 (RN-24 a RN-28) e do seed da V10. Os três mercados da V10
 * têm id maior que o de demonstração; companhias de outros ITs, maior ainda.
 */
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
        assertThat(count("""
                SELECT COUNT(*) FROM users u JOIN company c ON c.id = u.company_id
                WHERE u.created_at < '2026-10-30' AND u.role IN ('MANAGER', 'AUDITOR') AND c.type <> 'MARKET'
                """)).isZero();
        // RN-08 depois da V10: todo fornecedor do seed tem um ADMIN.
        assertThat(count("""
                SELECT COUNT(*) FROM company c
                WHERE c.type = 'SUPPLIER'
                  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.company_id = c.id AND u.role = 'ADMIN')
                  AND""" + SEEDED)).isZero();
    }

    // V10: o mercado de demonstração é esvaziado em três mercados PRO, cada um com ADMIN e dentro do limite.
    @Test
    void theSeedMarketsSplitTheDemoMarketUsersAndData() {
        assertThat(count("SELECT COUNT(*) FROM users WHERE company_id = " + DEMO_MARKET)).isZero();
        assertThat(count("SELECT COUNT(*) FROM batch WHERE market_id = " + DEMO_MARKET)).isZero();
        assertThat(count("SELECT COUNT(*) FROM report WHERE market_id = " + DEMO_MARKET)).isZero();
        assertThat(jdbcTemplate.queryForList("""
                SELECT CONCAT(c.name, ':', p.code, ':', COUNT(u.id), ':', SUM(u.role = 'ADMIN'))
                FROM company c
                JOIN company_plan cp ON cp.company_id = c.id AND cp.ended_at IS NULL
                JOIN plan p ON p.id = cp.plan_id
                LEFT JOIN users u ON u.company_id = c.id
                WHERE c.cnpj IN ('20202020000152', '30303030000138', '40404040000113')
                GROUP BY c.id, c.name, p.code
                """, String.class))
                .containsExactlyInAnyOrder("Mercado Bom Preço:PRO:8:1", "Supermercado Vale Verde:PRO:8:1", "Rede Feira Viva:PRO:8:1");
        // Os lotes e os relatórios de um fornecedor ficam no mesmo mercado.
        assertThat(count("""
                SELECT COUNT(*) FROM report r
                WHERE EXISTS (SELECT 1 FROM batch b WHERE b.supplier_id = r.supplier_id AND b.market_id <> r.market_id)
                """)).isZero();
        assertThat(count("SELECT COUNT(*) FROM audit_log WHERE affected_table = 'users' AND affected_entity_id > 22")).isZero();
    }

    @Test
    void batchesAndReportsBelongToTheMarketAndProductsToTheirSupplier() {
        assertThat(count("SELECT COUNT(*) FROM batch b JOIN product p ON p.id = b.product_id WHERE p.supplier_id <> b.supplier_id"))
                .isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM batch b JOIN company m ON m.id = b.market_id
                WHERE b.created_at < '2026-10-30' AND (b.status <> 'ACTIVE' OR m.type <> 'MARKET')
                """)).isZero();
        // RN-28: os 52 produtos do seed continuam existindo, com cópias para os fornecedores extras.
        assertThat(count("SELECT COUNT(*) FROM product WHERE id <= 52")).isEqualTo(52);
    }

    private long count(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
}
