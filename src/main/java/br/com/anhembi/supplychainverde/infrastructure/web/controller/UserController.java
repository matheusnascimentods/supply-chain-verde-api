package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.user.*;
import br.com.anhembi.supplychainverde.application.dto.pagination.OffsetPageResponseDTO;
import br.com.anhembi.supplychainverde.application.usecase.user.*;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
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
    private final SupplierRepository supplierRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Criar usuário", description = "Cria um usuário. supplierId é obrigatório quando role = SUPPLIER (fornecedor existente e ainda sem usuário) e proibido nos demais perfis (400). Email já cadastrado ou fornecedor já vinculado retornam 409. ADMIN cria qualquer perfil; MANAGER cria somente SUPPLIER (403 nos demais).")
    public UserResponseDTO create(@RequestBody UserRequestDTO request, @AuthenticationPrincipal CustomUserPrincipal principal) {
        if (isManager(principal) && (request == null || request.role() != UserRole.SUPPLIER)) {
            throw new AccessDeniedException("MANAGER pode criar somente usuários SUPPLIER.");
        }
        return register.execute(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Listar usuários", description = "Lista usuários de forma paginada, com filtro opcional de email parcial e sem diferenciar maiúsculas de minúsculas. Cada item traz supplierId e supplierName, nulos fora de SUPPLIER. ADMIN vê todos; MANAGER vê somente usuários SUPPLIER.")
    public OffsetPageResponseDTO<UserResponseDTO> list(
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return list.execute(email, isManager(principal) ? UserRole.SUPPLIER : null, limit, offset);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consultar usuário autenticado", description = "Retorna os dados do usuário associado ao token atual.")
    public UserResponseDTO me(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return userRepository.findById(principal.userId())
                .map(user -> new UserResponseDTO(user.getUserId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt(),
                        principal.supplierId(), supplierName(principal.supplierId())))
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + principal.userId()));
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
    @Operation(summary = "Atualizar perfil do usuário", description = "Altera o perfil de acesso de um usuário. Mudar de ou para SUPPLIER retorna 409, porque o SUPPLIER está sempre vinculado a um fornecedor. Requer ADMIN.")
    public UserResponseDTO updateRole(@PathVariable Long userId, @RequestBody UpdateUserRoleRequestDTO request) { return updateRole.execute(userId, request); }

    private boolean isManager(CustomUserPrincipal principal) {
        return "MANAGER".equals(principal.role());
    }

    private String supplierName(Long supplierId) {
        return supplierId == null ? null : supplierRepository.findById(supplierId).map(Supplier::getName).orElse(null);
    }
}
