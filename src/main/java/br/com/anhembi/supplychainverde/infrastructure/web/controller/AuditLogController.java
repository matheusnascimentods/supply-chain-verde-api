package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.pagination.OffsetPageResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.audit.AuditLogResponseDTO;
import br.com.anhembi.supplychainverde.application.usecase.audit.ListAuditLogsUseCase;
import br.com.anhembi.supplychainverde.domain.enums.AuditAction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Validated
@Tag(name = "Auditoria", description = "Consulta do histórico de ações auditadas.")
public class AuditLogController {
    private final ListAuditLogsUseCase list;

    @GetMapping
    @Operation(summary = "Listar logs de auditoria", description = "Filtra logs por período, ação e email, com paginação. Cada companhia vê só as ações feitas pelos próprios usuários; o PLATFORM_ADMIN vê todas.")
    public OffsetPageResponseDTO<AuditLogResponseDTO> list(
            @RequestParam("from") LocalDate from,
            @RequestParam("to") LocalDate to,
            @RequestParam(name = "action", required = false) AuditAction action,
            @RequestParam(name = "userEmail", required = false) String userEmail,
            @RequestParam(name = "limit", defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(name = "offset", defaultValue = "0") @Min(0) int offset,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return list.execute(from, to, action, userEmail, principal.companyId(), limit, offset);
    }
}
