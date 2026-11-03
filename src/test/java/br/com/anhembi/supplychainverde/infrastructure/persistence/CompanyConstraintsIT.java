package br.com.anhembi.supplychainverde.infrastructure.persistence;

import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static br.com.anhembi.supplychainverde.JdbcInserts.DEMO_MARKET;
import static br.com.anhembi.supplychainverde.JdbcInserts.insert;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** RNF-01: os vínculos de tipo e a assinatura ativa única são do banco, não só do código. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class CompanyConstraintsIT {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long supplierId;
    private long productId;

    @BeforeEach
    void setUp() {
        productId = jdbcTemplate.queryForObject("SELECT MIN(id) FROM product", Long.class);
        supplierId = jdbcTemplate.queryForObject("SELECT supplier_id FROM product WHERE id = ?", Long.class, productId);
    }

    @Test
    void batchMarketMustBeAMarket() {
        assertViolates("fk_batch_market", () -> batch(productId, supplierId, String.valueOf(supplierId), "'ACTIVE'", "NULL"));
    }

    @Test
    void batchProductMustBelongToTheBatchSupplier() {
        long otherSupplier = jdbcTemplate.queryForObject(
                "SELECT MIN(id) FROM company WHERE type = 'SUPPLIER' AND id <> ?", Long.class, supplierId);
        assertViolates("fk_batch_product_supplier", () -> batch(productId, otherSupplier, DEMO_MARKET, "'ACTIVE'", "NULL"));
    }

    @Test
    void rejectedBatchNeedsReasonAndActiveBatchNeedsProductionDate() {
        assertViolates("chk_batch_rejection_reason", () -> batch(productId, supplierId, DEMO_MARKET, "'REJECTED'", "NULL"));
        assertViolates("chk_batch_rejection_reason", () -> batch(productId, supplierId, DEMO_MARKET, "'PENDING'", "'motivo'"));
        assertViolates("chk_batch_active_produced_at", () -> jdbcTemplate.update("""
                INSERT INTO batch (product_id, supplier_id, market_id, status, quantity)
                VALUES (?, ?, %s, 'ACTIVE', 1)
                """.formatted(DEMO_MARKET), productId, supplierId));
    }

    @Test
    void companyHasOnlyOneActivePlan() {
        assertViolates("uq_company_plan_active", () -> jdbcTemplate.update(
                "INSERT INTO company_plan (company_id, plan_id) SELECT ?, id FROM plan WHERE code = 'PRO'", supplierId));
    }

    @Test
    void onlyPlatformAdminHasNoCompany() {
        assertViolates("chk_users_company", () -> jdbcTemplate.update(
                "INSERT INTO users (name, email, password, role) VALUES ('Sem companhia', 'sem@it.test', 'x', 'ADMIN')"));
        assertViolates("chk_users_company", () -> jdbcTemplate.update("""
                INSERT INTO users (name, email, password, role, company_id)
                VALUES ('Admin da plataforma', 'plataforma@it.test', 'x', 'PLATFORM_ADMIN', ?)
                """, supplierId));
    }

    @Test
    void productAndCertificationOnlyAcceptSuppliers() {
        assertViolates("fk_product_supplier", () -> jdbcTemplate.update(
                "INSERT INTO product (supplier_id, name, category, unit) VALUES (" + DEMO_MARKET + ", 'Produto', 'OTHER', 'KG')"));
        assertViolates("fk_certification_supplier", () -> jdbcTemplate.update("""
                INSERT INTO certification (supplier_id, certification, issuing_body, issued_at, expires_at, status)
                VALUES (%s, 'Orgânico', 'IBD', CURRENT_DATE, CURRENT_DATE, 'ACTIVE')
                """.formatted(DEMO_MARKET)));
    }

    private long batch(long product, long supplier, String marketSql, String statusSql, String reasonSql) {
        return insert(jdbcTemplate, """
                INSERT INTO batch (product_id, supplier_id, market_id, status, rejection_reason, quantity, produced_at)
                VALUES (?, ?, %s, %s, %s, 1, CURRENT_DATE)
                """.formatted(marketSql, statusSql, reasonSql), product, supplier);
    }

    // No MySQL o erro desfaz só a instrução; a transação do teste continua.
    private void assertViolates(String constraint, Runnable statement) {
        assertThatThrownBy(statement::run).hasMessageContaining(constraint);
    }
}
