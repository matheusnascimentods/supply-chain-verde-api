package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.audit.AuditLogResponseDTO;
import br.com.anhembi.supplychainverde.application.usecase.audit.ListAuditLogsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Auditoria", description = "Consulta do histórico de ações auditadas.")
public class AuditLogController {
    private final ListAuditLogsUseCase list;

    @GetMapping
    @Operation(summary = "Listar logs de auditoria", description = "Retorna o histórico de ações registradas no sistema.")
    public List<AuditLogResponseDTO> list() { return list.execute(); }
}
