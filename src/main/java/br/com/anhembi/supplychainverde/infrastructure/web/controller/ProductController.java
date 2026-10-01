package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.product.*;
import br.com.anhembi.supplychainverde.application.dto.pagination.OffsetPageResponseDTO;
import br.com.anhembi.supplychainverde.application.usecase.product.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Validated
@Tag(name = "Produtos", description = "Cadastro e consulta de produtos.")
public class ProductController {
    private final RegisterProductUseCase register;
    private final UpdateProductUseCase update;
    private final ListProductsUseCase list;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar produto", description = "Cadastra um produto rastreável.")
    public ProductResponseDTO create(@RequestBody ProductRequestDTO request) { return register.execute(request); }

    @PutMapping("/{productId}")
    @Operation(summary = "Atualizar produto", description = "Atualiza os dados de um produto.")
    public ProductResponseDTO update(@PathVariable Long productId, @RequestBody ProductRequestDTO request) { return update.execute(productId, request); }

    @GetMapping
    @Operation(summary = "Listar produtos", description = "Busca e pagina produtos por nome, descrição ou categoria.")
    public OffsetPageResponseDTO<ProductResponseDTO> list(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset,
            @RequestParam(required = false) String search
    ) {
        return list.execute(limit, offset, search);
    }
}
