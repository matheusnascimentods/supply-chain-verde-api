package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.plan.PlanResponseDTO;
import br.com.anhembi.supplychainverde.application.usecase.plan.ListPlansUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
@Tag(name = "Planos", description = "Catálogo de planos para o cadastro de companhias.")
public class PlanController {
    private final ListPlansUseCase listPlans;

    @GetMapping
    @Operation(summary = "Listar planos", description = "Público. Retorna o catálogo com code, name e maxUsers, do menor para o maior limite de usuários.")
    public List<PlanResponseDTO> list() { return listPlans.execute(); }
}
