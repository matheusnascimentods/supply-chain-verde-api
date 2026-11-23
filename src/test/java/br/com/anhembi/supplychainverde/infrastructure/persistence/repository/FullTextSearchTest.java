package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FullTextSearchTest {

    @Test
    void shouldRequireEveryTermAsPrefix() {
        assertThat(FullTextSearch.booleanQuery("  Café   orgânico ")).isEqualTo("+Café* +orgânico*");
        assertThat(FullTextSearch.booleanQuery("Grãos da")).isEqualTo("+Grãos* +da*");
    }

    @Test
    void shouldDropBooleanOperatorsInsteadOfApplyingThem() {
        assertThat(FullTextSearch.booleanQuery("\"agro-floresta\" -soja (milho) ~trigo@ <x> soja*"))
                .isEqualTo("+agro* +floresta* +soja* +milho* +trigo* +x* +soja*");
    }

    @Test
    void shouldKeepShortTermsAndStopwords() {
        assertThat(FullTextSearch.booleanQuery("c")).isEqualTo("+c*");
        assertThat(FullTextSearch.booleanQuery("ca")).isEqualTo("+ca*");
        assertThat(FullTextSearch.booleanQuery("com")).isEqualTo("+com*");
    }

    @Test
    void shouldKeepUnderscoreInsideTheWord() {
        assertThat(FullTextSearch.booleanQuery("joao_silva")).isEqualTo("+joao_silva*");
    }

    @Test
    void shouldBeEmptyWithoutLettersOrDigits() {
        assertThat(FullTextSearch.booleanQuery("--")).isEmpty();
        assertThat(FullTextSearch.booleanQuery("")).isEmpty();
    }

    @Test
    void shouldSearchCnpjByDigitPrefix() {
        assertThat(FullTextSearch.cnpjQuery("12.345")).isEqualTo("+12345*");
        assertThat(FullTextSearch.cnpjQuery("abc")).isEmpty();
    }
}
