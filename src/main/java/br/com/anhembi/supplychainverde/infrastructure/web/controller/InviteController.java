package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.auth.LoginResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.invite.AcceptInviteRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.invite.InviteDetailsResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.invite.SendInviteRequestDTO;
import br.com.anhembi.supplychainverde.application.usecase.invite.AcceptInviteUseCase;
import br.com.anhembi.supplychainverde.application.usecase.invite.GetInviteUseCase;
import br.com.anhembi.supplychainverde.application.usecase.invite.SendInviteUseCase;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invites")
@RequiredArgsConstructor
@Tag(name = "Convites", description = "Convite de usuários por email.")
public class InviteController {
    private final SendInviteUseCase send;
    private final GetInviteUseCase get;
    private final AcceptInviteUseCase accept;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Convidar usuário", description = "Somente o ADMIN da companhia. Envia por email um link de convite válido por 24h para a companhia de quem convida, com o perfil informado. Perfil não permitido para o tipo da companhia ou email inválido retornam 400; PLATFORM_ADMIN recebe 403. Email já cadastrado, convite ativo para o mesmo email ou companhia sem assento livre (usuários + convites ativos chegam a maxUsers) retornam 409. Falha no envio do email retorna 502 e nada é gravado.")
    public void send(@RequestBody SendInviteRequestDTO request, @AuthenticationPrincipal CustomUserPrincipal principal) {
        send.execute(request, principal.userId(), principal.companyId());
    }

    @GetMapping("/{token}")
    @Operation(summary = "Consultar convite", description = "Público. Devolve email, perfil, companhia e validade do convite, sem consumi-lo. Token desconhecido, expirado ou já aceito retorna 404, sem dizer qual dos três.")
    public InviteDetailsResponseDTO get(@PathVariable String token) {
        return get.execute(token);
    }

    @PostMapping("/accept")
    @Operation(summary = "Aceitar convite", description = "Público. Cria o usuário com o email e o perfil do convite, na companhia de quem convidou, e devolve o mesmo corpo do login. Nome ou senha vazios retornam 400; token desconhecido, expirado ou já aceito, 404; companhia acima do teto de assentos ou email já cadastrado, 409.")
    public LoginResponseDTO accept(@RequestBody AcceptInviteRequestDTO request) {
        return accept.execute(request);
    }
}
