package br.com.anhembi.supplychainverde.application.usecase.invite;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InviteTokenTest {
    @Test
    void generatesUrlSafeTokensWith256Bits() {
        String token = InviteToken.generate();

        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
        assertThat(InviteToken.generate()).isNotEqualTo(token);
    }

    @Test
    void hashIsDeterministicSha256Hex() {
        // SHA-256("abc"), vetor de teste do FIPS 180-2.
        assertThat(InviteToken.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
