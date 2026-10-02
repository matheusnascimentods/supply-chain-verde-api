package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.batch.*;
import br.com.anhembi.supplychainverde.application.usecase.batch.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "Lotes", description = "Cadastro e rastreabilidade de lotes.")
public class BatchController {
    private final RegisterBatchUseCase register;
    private final GetBatchTraceabilityUseCase traceability;
    private final ListBatchesUseCase list;

    @GetMapping("/batches")
    @Operation(
            summary = "Listar lotes",
            description = "Lista lotes paginados com currentStage e stages em ordem cronológica, incluindo endereços, transporte e emissão quando disponíveis. Lotes sem etapas retornam currentStage nulo e stages vazio. Os estágios são carregados em consultas agrupadas somente para os lotes da página. SUPPLIER consulta somente os lotes cujo supplierId corresponde ao userId do token."
    )
    public BatchPageResponseDTO list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Min(1) Long supplierId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        Long effectiveSupplierId = "SUPPLIER".equals(principal.role()) ? principal.userId() : supplierId;
        return list.execute(page, size, effectiveSupplierId);
    }

    @PostMapping("/batches")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar lote", description = "Cadastra um lote vinculado a um produto e fornecedor.")
    public BatchResponseDTO create(@RequestBody BatchRequestDTO request) { return register.execute(request); }

    @GetMapping("/batches/{batchId}/traceability")
    @Operation(summary = "Consultar rastreabilidade do lote", description = "Consulta pública da jornada completa de um lote.")
    @SecurityRequirements
    public BatchTraceabilityResponseDTO traceability(@PathVariable Long batchId) { return traceability.execute(batchId); }

}
