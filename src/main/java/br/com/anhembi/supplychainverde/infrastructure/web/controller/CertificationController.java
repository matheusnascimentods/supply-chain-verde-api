package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.certification.*;
import br.com.anhembi.supplychainverde.application.usecase.certification.*;
import br.com.anhembi.supplychainverde.domain.enums.CertificationStatus;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "Certificações", description = "Cadastro, atualização e consulta de certificações.")
public class CertificationController {
    private final RegisterCertificationUseCase register;
    private final UpdateCertificationStatusUseCase updateStatus;
    private final ListCertificationsUseCase list;

    @GetMapping("/certifications")
    @Operation(
            summary = "Listar certificações",
            description = "Lista certificações paginadas, opcionalmente filtradas por status; sem filtro, as mais recentes aparecem primeiro. A companhia fornecedora vê só as próprias; o mercado vê as de todos os fornecedores (catálogo)."
    )
    public CertificationPageResponseDTO list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) CertificationStatus status,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return list.execute(page, size, status, principal.supplierId());
    }

    @PostMapping("/suppliers/{supplierId}/certifications")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar certificação", description = "Somente o próprio fornecedor registra as certificações dele; outras companhias recebem 403 (RN-22).")
    public CertificationResponseDTO create(@PathVariable Long supplierId, @RequestBody CertificationRequestDTO request, @AuthenticationPrincipal CustomUserPrincipal principal) {
        // RN-22: o fornecedor cadastra só as próprias; o mercado não cadastra.
        if (principal.companyId() != null && !supplierId.equals(principal.supplierId())) {
            throw new AccessDeniedException("Fornecedor pode cadastrar certificações somente para si.");
        }
        return register.execute(supplierId, request);
    }

    @PatchMapping("/certifications/{certificationId}/status")
    @Operation(summary = "Atualizar status da certificação", description = "Somente o fornecedor dono da certificação (SUPPLIER ou ADMIN da companhia fornecedora); os demais recebem 403 (RN-22).")
    public CertificationResponseDTO updateStatus(@PathVariable Long certificationId, @RequestBody UpdateCertificationStatusRequestDTO request,
                                                 @AuthenticationPrincipal CustomUserPrincipal principal) {
        return updateStatus.execute(certificationId, request, principal.supplierId(), principal.companyId() == null);
    }
}
