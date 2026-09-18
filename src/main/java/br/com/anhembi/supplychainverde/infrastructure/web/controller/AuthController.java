package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.auth.*;
import br.com.anhembi.supplychainverde.application.usecase.user.AuthenticateUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Login e emissão de tokens JWT.")
public class AuthController {
    private final AuthenticateUserUseCase authenticate;

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário", description = "Valida as credenciais e retorna um token JWT.")
    @SecurityRequirements
    public LoginResponseDTO login(@RequestBody LoginRequestDTO request) { return authenticate.execute(request); }
}
