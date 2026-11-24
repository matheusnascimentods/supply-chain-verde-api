package br.com.anhembi.supplychainverde.infrastructure.persistence;

import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** V19: nenhum email do seed fica em domínio .example ou .local. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SeedEmailsIT {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void seedEmailsUseRealLookingDomains() {
        String placeholders = "SELECT email FROM %s WHERE email LIKE '%%.example.com' OR email LIKE '%%.local'";
        assertThat(jdbcTemplate.queryForList(placeholders.formatted("users"), String.class)).isEmpty();
        assertThat(jdbcTemplate.queryForList(placeholders.formatted("company"), String.class)).isEmpty();
    }

    @Test
    void companiesGetContactEmailFromTheirName() {
        assertThat(jdbcTemplate.queryForObject("SELECT email FROM company WHERE cnpj = '10000011101150'", String.class))
                .isEqualTo("contato@ervamatenativadoparana.com");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE email LIKE '%@psg.com'", Long.class))
                .isEqualTo(11);
    }
}
