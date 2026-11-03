package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.auth.LoginResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.company.CompanyResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.company.RegisterCompanyRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.company.UpdateOwnCompanyRequestDTO;
import br.com.anhembi.supplychainverde.application.usecase.company.GetOwnCompanyUseCase;
import br.com.anhembi.supplychainverde.application.usecase.company.RegisterCompanyUseCase;
import br.com.anhembi.supplychainverde.application.usecase.company.UpdateOwnCompanyUseCase;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@Tag(name = "Companhias", description = "Cadastro de mercados e fornecedores e dados da própria companhia.")
public class CompanyController {
    private final RegisterCompanyUseCase register;
    private final GetOwnCompanyUseCase getOwn;
    private final UpdateOwnCompanyUseCase updateOwn;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cadastrar companhia",
            description = "Público. Cria numa transação a companhia (type MARKET ou SUPPLIER), o endereço, a assinatura do plano planCode e o usuário ADMIN. A resposta é a mesma do POST /auth/login, então o administrador já entra logado. CNPJ ou email do administrador já cadastrados retornam 409; plano inexistente, CNPJ ou email inválidos retornam 400."
    )
    public LoginResponseDTO register(@RequestBody RegisterCompanyRequestDTO request) { return register.execute(request); }

    @GetMapping("/me")
    @Operation(summary = "Consultar a própria companhia", description = "Qualquer usuário com companhia. Retorna os dados da companhia, o endereço e o plano ativo. Usuário sem companhia (PLATFORM_ADMIN) recebe 403.")
    public CompanyResponseDTO me(@AuthenticationPrincipal CustomUserPrincipal principal) { return getOwn.execute(principal.userId()); }

    @PatchMapping("/me")
    @Operation(summary = "Atualizar a própria companhia", description = "Somente ADMIN. Campos opcionais, só os enviados mudam: name (razão social), email, phone e address (substituído por inteiro quando enviado). CNPJ e tipo não são editáveis. A resposta tem o mesmo formato de GET /companies/me.")
    public CompanyResponseDTO updateMe(@RequestBody UpdateOwnCompanyRequestDTO request,
                                       @AuthenticationPrincipal CustomUserPrincipal principal) {
        return updateOwn.execute(principal.userId(), request);
    }
}
