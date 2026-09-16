package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.user.*;
import br.com.anhembi.supplychainverde.application.usecase.user.*;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final RegisterUserUseCase register;
    private final UpdateUserRoleUseCase updateRole;
    private final UserRepository userRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponseDTO create(@RequestBody UserRequestDTO request) { return register.execute(request); }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserResponseDTO me(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return userRepository.findById(principal.userId())
                .map(user -> new UserResponseDTO(user.getUserId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt()))
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + principal.userId()));
    }

    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponseDTO updateRole(@PathVariable Long userId, @RequestBody UpdateUserRoleRequestDTO request) { return updateRole.execute(userId, request); }
}
