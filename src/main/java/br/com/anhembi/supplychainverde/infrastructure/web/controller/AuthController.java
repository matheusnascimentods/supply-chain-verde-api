package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.auth.*;
import br.com.anhembi.supplychainverde.application.usecase.user.AuthenticateUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticateUserUseCase authenticate;

    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody LoginRequestDTO request) { return authenticate.execute(request); }
}
