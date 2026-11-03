package br.com.anhembi.supplychainverde;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;

/** O MySQL não tem INSERT ... RETURNING: devolve o id gerado pelo AUTO_INCREMENT. */
public final class JdbcInserts {
    /** Mercado de demonstração criado pela V8. Depois da V10 ele fica sem usuários, lotes e relatórios. */
    public static final String DEMO_MARKET = "(SELECT MIN(id) FROM company WHERE type = 'MARKET')";

    /** Mercado Bom Preço (V10): tem usuários e os lotes e relatórios dos fornecedores com id múltiplo de 3. */
    public static final String SEED_MARKET = "(SELECT id FROM company WHERE cnpj = '20202020000152')";

    private JdbcInserts() {
    }

    public static long insert(JdbcTemplate jdbcTemplate, String sql, Object... args) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }
}
