package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierResponseDTO;
import br.com.anhembi.supplychainverde.application.usecase.supplier.*;
import br.com.anhembi.supplychainverde.domain.enums.ProductCategory;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@Validated
@Tag(name = "Fornecedores", description = "Cadastro, consulta e ranking de fornecedores.")
public class SupplierController {
    private final RegisterSupplierUseCase register;
    private final UpdateSupplierUseCase update;
    private final GetSupplierUseCase get;
    private final RankSuppliersBySustainabilityUseCase rank;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar fornecedor", description = "Cadastra um fornecedor e seu endereço.")
    public SupplierResponseDTO create(@RequestBody SupplierRequestDTO request) { return register.execute(request); }

    @PutMapping("/{supplierId}")
    @Operation(summary = "Atualizar fornecedor", description = "Atualiza os dados cadastrais de um fornecedor.")
    public SupplierResponseDTO update(@PathVariable Long supplierId, @RequestBody SupplierRequestDTO request) { return update.execute(supplierId, request); }

    @GetMapping
    @Operation(
            summary = "Consultar fornecedores",
            description = "Sem supplierId ou ranked=true, lista fornecedores; ranked=true retorna a mesma estrutura em ranking paginado. Cada item inclui supplierId, name, cnpj, address, phone, registeredAt, sustainabilityScore, activeCertificationCount, totalCo2Kg, reportCount e certifications. supplierId isolado retorna o detalhe cadastral. reportCount é uma contagem não negativa; certifications é sempre uma lista, vazia quando não houver certificações. Com ranked=true, productId ou o par category+unit ativa a recomendação: os fornecedores vêm ordenados pelo menor CO₂ médio por unidade daquele produto (ou categoria+unidade), com desempate por sustainabilityScore; quem não tem histórico vem no fim com co2KgPerUnit null. productId não combina com category/unit, e category exige unit. Esses parâmetros exigem ranked=true."
    )
    public Object getSuppliers(
            @RequestParam(required = false) @Min(1) Long supplierId,
            @RequestParam(required = false) Boolean ranked,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @Min(1) Long productId,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) ProductUnit unit
    ) {
        if (supplierId != null && ranked != null) {
            throw new IllegalArgumentException("supplierId e ranked são parâmetros mutuamente exclusivos.");
        }
        if (productId != null && (category != null || unit != null)) {
            throw new IllegalArgumentException("productId não pode ser combinado com category/unit.");
        }
        if ((category == null) != (unit == null)) {
            throw new IllegalArgumentException("category e unit devem ser informados juntos.");
        }
        if (!Boolean.TRUE.equals(ranked) && (productId != null || category != null)) {
            throw new IllegalArgumentException("productId, category e unit exigem ranked=true.");
        }
        if (supplierId != null) {
            return get.execute(supplierId);
        }
        if (Boolean.TRUE.equals(ranked)) {
            return productId != null || category != null
                    ? rank.executeRecommended(productId, category, unit, limit, offset, search)
                    : rank.execute(limit, offset, search);
        }
        return rank.executeAll();
    }
}
