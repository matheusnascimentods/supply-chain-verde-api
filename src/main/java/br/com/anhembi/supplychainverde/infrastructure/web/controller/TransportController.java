package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.transport.*;
import br.com.anhembi.supplychainverde.application.usecase.transport.RegisterTransportUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stages/{chainId}/transport")
@RequiredArgsConstructor
public class TransportController {
    private final RegisterTransportUseCase register;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransportResponseDTO create(@PathVariable Long chainId, @RequestBody TransportRequestDTO request) {
        if (request == null || request.chainId() == null || !chainId.equals(request.chainId())) {
            throw new IllegalArgumentException("chainId do path e do corpo devem coincidir.");
        }
        return register.execute(request);
    }
}
