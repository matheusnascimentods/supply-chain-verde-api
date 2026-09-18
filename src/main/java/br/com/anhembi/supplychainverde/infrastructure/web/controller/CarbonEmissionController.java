package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.carbonemission.*;
import br.com.anhembi.supplychainverde.application.usecase.emission.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Emissões de carbono", description = "Cálculo e consulta da pegada de carbono.")
public class CarbonEmissionController {
    private final CalculateCarbonEmissionUseCase calculate;
    private final GetBatchCarbonFootprintUseCase footprint;

    @PostMapping("/stages/{chainId}/emission")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Calcular emissão", description = "Calcula e registra a emissão de carbono de uma etapa.")
    public CarbonEmissionResponseDTO calculate(@PathVariable Long chainId, @RequestBody CarbonEmissionRequestDTO request) { return calculate.execute(chainId, request); }

    @GetMapping("/batches/{batchId}/carbon-footprint")
    @Operation(summary = "Consultar pegada de carbono", description = "Consulta pública das emissões acumuladas de um lote.")
    @SecurityRequirements
    public CarbonFootprintResponseDTO footprint(@PathVariable Long batchId) { return footprint.execute(batchId); }
}
