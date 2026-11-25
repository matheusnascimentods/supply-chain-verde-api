package br.com.anhembi.supplychainverde.infrastructure.persistence;

import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/** V21 (018): destino obrigatório, varejo no mercado, sem origin_address_id e auditoria de chain funcionando. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigrationStageAddressIT {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void everyStageHasADestinationAndRetailEndsAtTheMarket() {
        Long withoutDestination = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chain WHERE destination_address_id IS NULL", Long.class);
        Long retailOutsideTheMarket = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM chain c
                JOIN batch b ON b.id = c.batch_id
                JOIN company m ON m.id = b.market_id
                WHERE c.stage_type = 'RETAIL' AND c.destination_address_id <> m.address_id
                """, Long.class);

        assertThat(withoutDestination).isZero();
        assertThat(retailOutsideTheMarket).isZero();
    }

    @Test
    void originColumnIsGoneAndDestinationIsRequired() {
        Long originColumns = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'chain' AND column_name = 'origin_address_id'
                """, Long.class);
        String destinationNullable = jdbcTemplate.queryForObject("""
                SELECT is_nullable FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'chain' AND column_name = 'destination_address_id'
                """, String.class);

        assertThat(originColumns).isZero();
        assertThat(destinationNullable).isEqualTo("NO");
    }

    @Test
    @Transactional
    void recreatedTriggersStillAuditChainUpdates() {
        Long chainId = jdbcTemplate.queryForObject("SELECT MIN(id) FROM chain", Long.class);
        jdbcTemplate.update("UPDATE chain SET ended_at = COALESCE(ended_at, started_at) + INTERVAL 1 SECOND WHERE id = ?", chainId);

        String after = jdbcTemplate.queryForObject("""
                SELECT after_data FROM audit_log
                WHERE affected_table = 'chain' AND affected_entity_id = ? AND action = 'UPDATE'
                ORDER BY id DESC LIMIT 1
                """, String.class, chainId);

        assertThat(after).contains("ended_at").doesNotContain("origin_address_id");
    }
}
