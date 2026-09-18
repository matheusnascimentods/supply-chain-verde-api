package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.certification.*;
import br.com.anhembi.supplychainverde.application.usecase.certification.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Certificações", description = "Cadastro, atualização e consulta de certificações.")
public class CertificationController {
    private final RegisterCertificationUseCase register;
    private final UpdateCertificationStatusUseCase updateStatus;
    private final ListExpiringCertificationsUseCase expiring;

    @PostMapping("/suppliers/{supplierId}/certifications")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar certificação", description = "Registra uma certificação ambiental para um fornecedor.")
    public CertificationResponseDTO create(@PathVariable Long supplierId, @RequestBody CertificationRequestDTO request) { return register.execute(supplierId, request); }

    @PatchMapping("/certifications/{certificationId}/status")
    @Operation(summary = "Atualizar status da certificação", description = "Atualiza o status de uma certificação.")
    public CertificationResponseDTO updateStatus(@PathVariable Long certificationId, @RequestBody UpdateCertificationStatusRequestDTO request) { return updateStatus.execute(certificationId, request); }

    @GetMapping("/certifications/expiring")
    @Operation(summary = "Listar certificações próximas do vencimento", description = "Retorna certificações que estão próximas do vencimento.")
    public List<CertificationResponseDTO> expiring() { return expiring.execute(); }
}
