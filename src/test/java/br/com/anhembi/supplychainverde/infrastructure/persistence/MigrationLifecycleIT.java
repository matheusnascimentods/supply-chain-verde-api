package br.com.anhembi.supplychainverde.infrastructure.persistence;

import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigrationLifecycleIT {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void legacyRetailBatchesAreCompleted() {
        Long activeWithRetail = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM batch b
                WHERE b.status = 'ACTIVE'
                  AND EXISTS (SELECT 1 FROM chain c
                              WHERE c.batch_id = b.id AND c.stage_type = 'RETAIL')
                """, Long.class);

        assertThat(activeWithRetail).isZero();
    }

    @Test
    void seedStagesHaveEligibleResponsibleOwnersOrNoEligibleOwner() {
        Long wrongOwner = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM chain c
                JOIN batch b ON b.id = c.batch_id
                JOIN users u ON u.id = c.responsible_user_id
                WHERE NOT (u.company_id <=> IF(c.stage_type = 'RETAIL', b.market_id, b.supplier_id))
                  AND EXISTS (
                      SELECT 1 FROM users eligible
                      WHERE eligible.company_id = IF(c.stage_type = 'RETAIL', b.market_id, b.supplier_id)
                        AND eligible.is_active
                        AND CASE
                            WHEN c.stage_type = 'RETAIL' THEN eligible.role IN ('ADMIN', 'MANAGER')
                            ELSE eligible.role IN ('ADMIN', 'SUPPLIER')
                        END
                  )
                """, Long.class);

        assertThat(wrongOwner).isZero();
    }
}
