package br.com.anhembi.supplychainverde.application.usecase.invite;

import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Conteúdo do email de convite. O HTML fica em templates/invite-email.html, com %1$s (companhia), %2$s (perfil) e
 * %3$s (link); o logo vem por cid:logo, anexado pelo SendGridEmailSender. O nome da companhia vem do cadastro dela, então é escapado.
 */
public record InviteEmail(String subject, String html) {
    private static final String TEMPLATE = load("/templates/invite-email.html");

    public static InviteEmail of(String companyName, UserRole role, String link) {
        return new InviteEmail("Convite para a " + companyName + " no Supply Chain Verde",
                TEMPLATE.formatted(HtmlUtils.htmlEscape(companyName), label(role), HtmlUtils.htmlEscape(link)));
    }

    private static String load(String path) {
        try (InputStream in = InviteEmail.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Template não encontrado: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String label(UserRole role) {
        return switch (role) {
            case ADMIN -> "Administrador";
            case MANAGER -> "Gestor";
            case AUDITOR -> "Auditor";
            case SUPPLIER -> "Fornecedor";
            case PLATFORM_ADMIN -> throw new IllegalArgumentException("PLATFORM_ADMIN não é convidado.");
        };
    }
}
