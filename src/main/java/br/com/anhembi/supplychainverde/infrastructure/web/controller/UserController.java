package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.user.*;
import br.com.anhembi.supplychainverde.application.dto.pagination.OffsetPageResponseDTO;
import br.com.anhembi.supplychainverde.application.usecase.user.*;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.application.mapper.UserDtoMapper;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "Usuários", description = "Administração de usuários e perfis de acesso.")
public class UserController {
    private final RegisterUserUseCase register;
    private final UpdateUserRoleUseCase updateRole;
    private final ListUsersUseCase list;
    private final UpdateOwnUserUseCase updateOwn;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final UserDtoMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar usuário", description = "Somente o ADMIN da companhia (RN-09). O usuário nasce na companhia de quem cria. Perfis por tipo (RN-07): MARKET aceita ADMIN, MANAGER e AUDITOR; SUPPLIER aceita ADMIN e SUPPLIER; outro perfil retorna 400. Email já cadastrado ou limite de usuários do plano atingido (RN-04) retornam 409. O PLATFORM_ADMIN não tem companhia e recebe 403.")
    public UserResponseDTO create(@RequestBody UserRequestDTO request, @AuthenticationPrincipal CustomUserPrincipal principal) {
        return register.execute(request, principal.companyId());
    }

    @GetMapping
    @Operation(summary = "Listar usuários", description = "Qualquer usuário autenticado lista os usuários da própria companhia (RN-13b), paginados e com filtro opcional de email parcial, sem diferenciar maiúsculas de minúsculas. O PLATFORM_ADMIN lista todos. Cada item traz companyId, companyType e companyName; supplierId e supplierName repetem a companhia quando ela é fornecedora.")
    public OffsetPageResponseDTO<UserResponseDTO> list(
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return list.execute(email, principal.companyId(), limit, offset);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consultar usuário autenticado", description = "Retorna os dados do usuário do token, com companyId, companyType e companyName (nulos para o PLATFORM_ADMIN). supplierId e supplierName repetem a companhia quando ela é fornecedora (RF-04).")
    public UserResponseDTO me(@AuthenticationPrincipal CustomUserPrincipal principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + principal.userId()));
        Company company = user.getCompanyId() == null ? null : companyRepository.findById(user.getCompanyId()).orElse(null);
        return mapper.toResponse(user, company);
    }

    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Atualizar o próprio usuário",
            description = "Qualquer perfil autenticado. Atualiza o usuário do token. Todos os campos são opcionais e só os enviados são alterados: userName e newPassword, que exige currentPassword. Senha atual ausente ou incorreta, ou userName/newPassword vazios, retornam 400 sem alterar nada. Email e perfil não são editáveis e são ignorados se enviados. A resposta tem o mesmo formato de GET /users/me."
    )
    public UserResponseDTO updateMe(@RequestBody UpdateOwnUserRequestDTO request,
                                    @AuthenticationPrincipal CustomUserPrincipal principal) {
        updateOwn.execute(principal.userId(), request);
        return me(principal);
    }

    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar perfil do usuário", description = "Somente ADMIN, e só na própria companhia (outra companhia retorna 403); o PLATFORM_ADMIN altera qualquer uma. O novo perfil precisa ser permitido para o tipo da companhia (RN-07) e não pode ser PLATFORM_ADMIN (400). Deixar a companhia sem ADMIN, inclusive o único admin trocando o próprio perfil, retorna 409 (RN-08).")
    public UserResponseDTO updateRole(@PathVariable Long userId, @RequestBody UpdateUserRoleRequestDTO request,
                                      @AuthenticationPrincipal CustomUserPrincipal principal) {
        return updateRole.execute(userId, request, principal.companyId());
    }
}
