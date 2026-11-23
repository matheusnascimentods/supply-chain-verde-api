package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Monta o termo de MATCH ... AGAINST (... IN BOOLEAN MODE). */
public final class FullTextSearch {
    // O índice não guarda palavras com menos de 3 letras (innodb_ft_min_token_size) nem as stopwords padrão
    // (INFORMATION_SCHEMA.INNODB_FT_DEFAULT_STOPWORD). Obrigatória, uma palavra dessas zera o resultado
    // quando ela aparece só inteira no dado (o "h" e o "com" de h.kane@bayern.com).
    private static final int MIN_TOKEN_SIZE = 3;
    private static final Set<String> STOPWORDS = Set.of(
            "a", "about", "an", "are", "as", "at", "be", "by", "com", "de", "en", "for", "from", "how", "i", "in",
            "is", "it", "la", "of", "on", "or", "that", "the", "this", "to", "was", "what", "when", "where", "who",
            "will", "with", "und", "www");

    private FullTextSearch() {
    }

    /**
     * RN-01/RN-02: cada palavra casa por prefixo e nada é descartado. A palavra que o índice não guarda inteira
     * vira opcional quando há outra obrigatória; sozinha, continua obrigatória ("c" filtra por prefixo).
     */
    public static String booleanQuery(String search) {
        List<String> terms = Arrays.stream(search.replaceAll("[^\\p{L}\\p{N}_]+", " ").trim().split(" "))
                .filter(term -> !term.isEmpty())
                .toList();
        boolean hasIndexable = terms.stream().anyMatch(FullTextSearch::indexable);
        return terms.stream()
                .map(term -> (indexable(term) || !hasIndexable ? "+" : "") + term + "*")
                .collect(Collectors.joining(" "));
    }

    /** RN-04: só os dígitos, juntos, como um prefixo; vazio quando não há dígito. */
    public static String cnpjQuery(String search) {
        String digits = search.replaceAll("\\D", "");
        return digits.isEmpty() ? "" : "+" + digits + "*";
    }

    private static boolean indexable(String term) {
        return term.length() >= MIN_TOKEN_SIZE && !STOPWORDS.contains(term.toLowerCase(Locale.ROOT));
    }
}
