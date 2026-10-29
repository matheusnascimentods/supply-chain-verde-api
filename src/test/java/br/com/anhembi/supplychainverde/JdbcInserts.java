package br.com.anhembi.supplychainverde;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;

/** O MySQL não tem INSERT ... RETURNING: devolve o id gerado pelo AUTO_INCREMENT. */
public final class JdbcInserts {
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
