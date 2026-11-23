package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Monta o termo de MATCH ... AGAINST (... IN BOOLEAN MODE). */
public final class FullTextSearch {
    private FullTextSearch() {
    }

    /** RN-01/RN-02: cada palavra é obrigatória e casa por prefixo; nada é descartado (o MySQL não corta termo com '*'). */
    public static String booleanQuery(String search) {
        return Arrays.stream(search.replaceAll("[^\\p{L}\\p{N}_]+", " ").trim().split(" "))
                .filter(term -> !term.isEmpty())
                .map(term -> "+" + term + "*")
                .collect(Collectors.joining(" "));
    }

    /** RN-04: só os dígitos, juntos, como um prefixo; vazio quando não há dígito. */
    public static String cnpjQuery(String search) {
        String digits = search.replaceAll("\\D", "");
        return digits.isEmpty() ? "" : "+" + digits + "*";
    }
}
