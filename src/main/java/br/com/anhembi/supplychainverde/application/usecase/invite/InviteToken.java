package br.com.anhembi.supplychainverde.application.usecase.invite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Token do link de convite: 256 bits aleatórios em base64url. O banco guarda só o SHA-256 (hex), então
 * um vazamento do banco não expõe links válidos (OWASP Forgot Password Cheat Sheet).
 */
public final class InviteToken {
    private static final SecureRandom RANDOM = new SecureRandom();

    private InviteToken() {
    }

    public static String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível na JVM.", exception);
        }
    }
}
