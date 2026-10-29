package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Monta o termo de MATCH ... AGAINST (... IN BOOLEAN MODE): cada palavra é obrigatória e casa por prefixo. */
public final class FullTextSearch {
    // O InnoDB não indexa palavras com menos de 3 letras (innodb_ft_min_token_size) nem as stopwords padrão.
    // Com "+", uma palavra dessas zera o resultado em vez de ser ignorada.
    private static final int MIN_TOKEN_SIZE = 3;
    private static final Set<String> STOPWORDS = Set.of(
            "about", "are", "com", "for", "from", "how", "that", "the", "this",
            "und", "was", "what", "when", "where", "who", "will", "with", "www");

    private FullTextSearch() {
    }

    public static String booleanQuery(String search) {
        return Arrays.stream(search.replaceAll("[^\\p{L}\\p{N}]+", " ").trim().split(" "))
                .filter(term -> term.length() >= MIN_TOKEN_SIZE && !STOPWORDS.contains(term.toLowerCase(Locale.ROOT)))
                .map(term -> "+" + term + "*")
                .collect(Collectors.joining(" "));
    }
}
