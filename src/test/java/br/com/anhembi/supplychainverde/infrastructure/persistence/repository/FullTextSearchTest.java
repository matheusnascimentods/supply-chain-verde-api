package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FullTextSearchTest {

    @Test
    void shouldRequireEveryTermAsPrefix() {
        assertThat(FullTextSearch.booleanQuery("  Café   orgânico ")).isEqualTo("+Café* +orgânico*");
    }

    @Test
    void shouldDropBooleanOperatorsInsteadOfApplyingThem() {
        assertThat(FullTextSearch.booleanQuery("\"agro-floresta\" -soja (milho) ~trigo@ <x> soja*"))
                .isEqualTo("+agro* +floresta* +soja* +milho* +trigo* +soja*");
    }

    @Test
    void shouldDropTermsTheIndexDoesNotStore() {
        assertThat(FullTextSearch.booleanQuery("café com leite de SP")).isEqualTo("+café* +leite*");
        assertThat(FullTextSearch.booleanQuery("THE")).isEmpty();
        assertThat(FullTextSearch.booleanQuery("")).isEmpty();
    }
}
