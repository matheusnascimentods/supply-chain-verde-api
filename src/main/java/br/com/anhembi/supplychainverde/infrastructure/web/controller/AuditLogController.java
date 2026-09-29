package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.audit.AuditLogPageDTO;
import br.com.anhembi.supplychainverde.application.usecase.audit.ListAuditLogsUseCase;
import br.com.anhembi.supplychainverde.domain.enums.AuditAction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
    @Operation(summary = "Listar logs de auditoria", description = "Filtra logs por período, ação e email, com paginação.")
    public AuditLogPageDTO list(
            @RequestParam("from") LocalDate from,
            @RequestParam("to") LocalDate to,
            @RequestParam(name = "action", required = false) AuditAction action,
            @RequestParam(name = "userEmail", required = false) String userEmail,
            @RequestParam(name = "limit", defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(name = "offset", defaultValue = "0") @Min(0) int offset
    ) {
        return list.execute(from, to, action, userEmail, limit, offset);
    }
}
