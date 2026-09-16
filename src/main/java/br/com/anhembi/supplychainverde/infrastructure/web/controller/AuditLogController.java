package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.audit.AuditLogResponseDTO;
import br.com.anhembi.supplychainverde.application.usecase.audit.ListAuditLogsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {
    private final ListAuditLogsUseCase list;

    @GetMapping
    public List<AuditLogResponseDTO> list() { return list.execute(); }
}
