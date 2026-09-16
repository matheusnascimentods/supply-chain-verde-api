package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.chain.*;
import br.com.anhembi.supplychainverde.application.usecase.chain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/batches/{batchId}/stages")
@RequiredArgsConstructor
public class ChainController {
    private final RegisterChainStageUseCase register;
    private final ListChainStagesByBatchUseCase list;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChainResponseDTO create(@PathVariable Long batchId, @RequestHeader("X-Responsible-User-Id") Long responsibleUserId, @RequestBody ChainRequestDTO request) {
        return register.execute(batchId, responsibleUserId, request);
    }

    @GetMapping
    public List<ChainResponseDTO> list(@PathVariable Long batchId) { return list.execute(batchId); }
}
