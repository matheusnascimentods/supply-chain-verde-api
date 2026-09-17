package br.com.anhembi.supplychainverde.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CnpjTest {

    @Test
    void shouldNormalizeAndFormatAValidCnpj() {
        Cnpj cnpj = new Cnpj("11.222.333/0001-81");

        assertThat(cnpj.value()).isEqualTo("11222333000181");
        assertThat(cnpj.formatted()).isEqualTo("11.222.333/0001-81");
        assertThat(Cnpj.isValid("11.222.333/0001-81")).isTrue();
    }

    @Test
    void shouldRejectInvalidCnpjs() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Cnpj(null));
        assertThatIllegalArgumentException().isThrownBy(() -> new Cnpj("00000000000000"));
        assertThatIllegalArgumentException().isThrownBy(() -> new Cnpj("11.222.333/0001-82"));
        assertThat(Cnpj.isValid("not-a-cnpj")).isFalse();
    }
}
