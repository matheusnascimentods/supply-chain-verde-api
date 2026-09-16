package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.user.*;
import br.com.anhembi.supplychainverde.application.usecase.user.*;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public UserResponseDTO create(@RequestBody UserRequestDTO request) { return register.execute(request); }

    @GetMapping("/me")
    public UserResponseDTO me(@RequestHeader("X-User-Id") Long userId) {
        return userRepository.findById(userId)
                .map(user -> new UserResponseDTO(user.getUserId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt()))
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + userId));
    }

    @PatchMapping("/{userId}/role")
    public UserResponseDTO updateRole(@PathVariable Long userId, @RequestBody UpdateUserRoleRequestDTO request) { return updateRole.execute(userId, request); }
}
