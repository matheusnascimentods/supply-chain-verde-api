package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.user.*;
import br.com.anhembi.supplychainverde.application.usecase.user.*;
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
    private final UserRepository userRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar usuário", description = "Cria um usuário. Requer perfil ADMIN.")
    public UserResponseDTO create(@RequestBody UserRequestDTO request) { return register.execute(request); }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar usuários", description = "Lista usuários de forma paginada, com filtro opcional de email parcial e sem diferenciar maiúsculas de minúsculas. Requer perfil ADMIN.")
    public UserPageDTO list(
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset
    ) {
        return list.execute(email, limit, offset);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consultar usuário autenticado", description = "Retorna os dados do usuário associado ao token atual.")
    public UserResponseDTO me(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return userRepository.findById(principal.userId())
                .map(user -> new UserResponseDTO(user.getUserId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt()))
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + principal.userId()));
    }

    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar perfil do usuário", description = "Altera o perfil de acesso de um usuário. Requer ADMIN.")
    public UserResponseDTO updateRole(@PathVariable Long userId, @RequestBody UpdateUserRoleRequestDTO request) { return updateRole.execute(userId, request); }
}
