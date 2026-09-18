package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.transport.*;
import br.com.anhembi.supplychainverde.application.usecase.transport.RegisterTransportUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stages/{chainId}/transport")
@RequiredArgsConstructor
@Tag(name = "Transportes", description = "Registro dos transportes utilizados em etapas da cadeia.")
public class TransportController {
    private final RegisterTransportUseCase register;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar transporte", description = "Registra os dados de transporte de uma etapa.")
    public TransportResponseDTO create(@PathVariable Long chainId, @RequestBody TransportRequestDTO request) {
        if (request == null || request.chainId() == null || !chainId.equals(request.chainId())) {
            throw new IllegalArgumentException("chainId do path e do corpo devem coincidir.");
        }
        return register.execute(request);
    }
}
