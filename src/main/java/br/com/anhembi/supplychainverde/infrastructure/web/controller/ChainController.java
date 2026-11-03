package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import br.com.anhembi.supplychainverde.application.dto.chain.*;
import br.com.anhembi.supplychainverde.application.usecase.chain.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/batches/{batchId}/stages")
@RequiredArgsConstructor
@Tag(name = "Etapas da cadeia", description = "Registro e consulta das etapas percorridas por um lote.")
public class ChainController {
    private final RegisterChainStageUseCase register;
    private final ListChainStagesByBatchUseCase list;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar etapa", description = "Registra a próxima etapa da cadeia de um lote do mercado ou do fornecedor do usuário (outro lote: 403). Em lote PENDING, só o fornecedor registra, e só PRODUCTION, que muda o lote para ACTIVE e preenche producedAt com a data de startedAt. Lote REJECTED retorna 409. O usuário de X-Responsible-User-Id precisa ser do mercado ou do fornecedor do lote (403).")
    public ChainResponseDTO create(@PathVariable Long batchId, @RequestHeader("X-Responsible-User-Id") Long responsibleUserId,
                                   @RequestBody ChainRequestDTO request, @AuthenticationPrincipal CustomUserPrincipal principal) {
        return register.execute(batchId, responsibleUserId, request, principal.marketId(), principal.supplierId());
    }

    @GetMapping
    @Operation(summary = "Listar etapas do lote", description = "Retorna as etapas de um lote em ordem.")
    public List<ChainResponseDTO> list(@PathVariable Long batchId) { return list.execute(batchId); }
}
