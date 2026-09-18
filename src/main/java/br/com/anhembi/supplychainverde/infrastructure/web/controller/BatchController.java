package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.batch.*;
import br.com.anhembi.supplychainverde.application.usecase.batch.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Lotes", description = "Cadastro e rastreabilidade de lotes.")
public class BatchController {
    private final RegisterBatchUseCase register;
    private final GetBatchTraceabilityUseCase traceability;
    private final ListBatchesBySupplierUseCase bySupplier;

    @PostMapping("/batches")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar lote", description = "Cadastra um lote vinculado a um produto e fornecedor.")
    public BatchResponseDTO create(@RequestBody BatchRequestDTO request) { return register.execute(request); }

    @GetMapping("/batches/{batchId}/traceability")
    @Operation(summary = "Consultar rastreabilidade do lote", description = "Consulta pública da jornada completa de um lote.")
    @SecurityRequirements
    public BatchTraceabilityResponseDTO traceability(@PathVariable Long batchId) { return traceability.execute(batchId); }

    @GetMapping("/suppliers/{supplierId}/batches")
    @Operation(summary = "Listar lotes do fornecedor", description = "Retorna os lotes cadastrados para um fornecedor.")
    public List<BatchResponseDTO> bySupplier(@PathVariable Long supplierId) { return bySupplier.execute(supplierId); }
}
