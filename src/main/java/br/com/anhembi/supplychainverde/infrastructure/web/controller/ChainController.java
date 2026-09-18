package br.com.anhembi.supplychainverde.infrastructure.web.controller;

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
    @Operation(summary = "Registrar etapa", description = "Registra uma nova etapa da cadeia para um lote.")
    public ChainResponseDTO create(@PathVariable Long batchId, @RequestHeader("X-Responsible-User-Id") Long responsibleUserId, @RequestBody ChainRequestDTO request) {
        return register.execute(batchId, responsibleUserId, request);
    }

    @GetMapping
    @Operation(summary = "Listar etapas do lote", description = "Retorna as etapas de um lote em ordem.")
    public List<ChainResponseDTO> list(@PathVariable Long batchId) { return list.execute(batchId); }
}
