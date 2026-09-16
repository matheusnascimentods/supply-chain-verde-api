package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.carbonemission.*;
import br.com.anhembi.supplychainverde.application.usecase.emission.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CarbonEmissionController {
    private final CalculateCarbonEmissionUseCase calculate;
    private final GetBatchCarbonFootprintUseCase footprint;

    @PostMapping("/stages/{chainId}/emission")
    @ResponseStatus(HttpStatus.CREATED)
    public CarbonEmissionResponseDTO calculate(@PathVariable Long chainId, @RequestBody CarbonEmissionRequestDTO request) { return calculate.execute(chainId, request); }

    @GetMapping("/batches/{batchId}/carbon-footprint")
    public CarbonFootprintResponseDTO footprint(@PathVariable Long batchId) { return footprint.execute(batchId); }
}
