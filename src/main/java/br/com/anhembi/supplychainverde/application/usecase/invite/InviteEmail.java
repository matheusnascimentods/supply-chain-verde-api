package br.com.anhembi.supplychainverde.application.usecase.invite;

import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import org.springframework.web.util.HtmlUtils;

/** Conteúdo do email de convite. O nome da companhia vem do cadastro dela, então é escapado. */
public record InviteEmail(String subject, String html) {
    public static InviteEmail of(String companyName, UserRole role, String link) {
        String company = HtmlUtils.htmlEscape(companyName);
        return new InviteEmail("Convite para a " + companyName + " no Supply Chain Verde", """
                <p>Olá!</p>
                <p>Você foi convidado para entrar na <strong>%s</strong> no Supply Chain Verde com o perfil <strong>%s</strong>.</p>
                <p><a href="%s">Aceitar convite</a></p>
                <p>O link vale por 24 horas e só pode ser usado uma vez. Se não esperava este convite, ignore este email.</p>
                """.formatted(company, label(role), HtmlUtils.htmlEscape(link)));
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
