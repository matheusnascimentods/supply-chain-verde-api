package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.batch.*;
import br.com.anhembi.supplychainverde.application.usecase.batch.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BatchController {
    private final RegisterBatchUseCase register;
    private final GetBatchTraceabilityUseCase traceability;
    private final ListBatchesBySupplierUseCase bySupplier;

    @PostMapping("/batches")
    @ResponseStatus(HttpStatus.CREATED)
    public BatchResponseDTO create(@RequestBody BatchRequestDTO request) { return register.execute(request); }

    @GetMapping("/batches/{batchId}/traceability")
    public BatchTraceabilityResponseDTO traceability(@PathVariable Long batchId) { return traceability.execute(batchId); }

    @GetMapping("/suppliers/{supplierId}/batches")
    public List<BatchResponseDTO> bySupplier(@PathVariable Long supplierId) { return bySupplier.execute(supplierId); }
}
