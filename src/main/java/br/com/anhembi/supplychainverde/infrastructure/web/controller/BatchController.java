package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.batch.*;
import br.com.anhembi.supplychainverde.application.usecase.batch.*;
import br.com.anhembi.supplychainverde.domain.enums.BatchStatus;
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
    private final RejectBatchUseCase reject;

    @GetMapping("/batches")
    @Operation(
            summary = "Listar lotes",
            description = "Lista lotes paginados com currentStage e stages em ordem cronológica, incluindo endereços, transporte e emissão quando disponíveis. Lotes sem etapas retornam currentStage nulo e stages vazio. Os estágios são carregados em consultas agrupadas somente para os lotes da página. O mercado vê só os próprios lotes, e a companhia fornecedora só os lotes dela; supplierId e status (PENDING, ACTIVE ou REJECTED) filtram dentro desse escopo. Cada item traz status, rejectionReason, marketId e marketName."
    )
    public BatchPageResponseDTO list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Min(1) Long supplierId,
            @RequestParam(required = false) BatchStatus status,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        Long effectiveSupplierId = principal.isSupplierCompany() ? principal.supplierId() : supplierId;
        return list.execute(page, size, principal.marketId(), effectiveSupplierId, status);
    }

    @PostMapping("/batches")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Pedir lote", description = "Somente mercado (MANAGER ou ADMIN). Recebe productId, supplierId e quantity; o mercado vem do token. O produto precisa ser do fornecedor informado (400). O lote nasce PENDING e sem producedAt, que é preenchido pela etapa PRODUCTION (RN-17 a RN-20). Outras companhias recebem 403.")
    public BatchResponseDTO create(@RequestBody BatchRequestDTO request, @AuthenticationPrincipal CustomUserPrincipal principal) {
        return register.execute(request, principal.marketId());
    }

    @PatchMapping("/batches/{batchId}/reject")
    @Operation(summary = "Recusar lote", description = "Somente o fornecedor do lote, com reason obrigatório. O lote vira REJECTED, que é final. Lote fora de PENDING retorna 409; de outro fornecedor, 403 (RN-19).")
    public BatchResponseDTO reject(@PathVariable Long batchId, @RequestBody RejectBatchRequestDTO request,
                                   @AuthenticationPrincipal CustomUserPrincipal principal) {
        return reject.execute(batchId, request == null ? null : request.reason(), principal.supplierId());
    }

    @GetMapping("/batches/{batchId}/traceability")
    @Operation(summary = "Consultar rastreabilidade do lote", description = "Consulta pública da jornada completa de um lote.")
    @SecurityRequirements
    public BatchTraceabilityResponseDTO traceability(@PathVariable Long batchId) { return traceability.execute(batchId); }

}
