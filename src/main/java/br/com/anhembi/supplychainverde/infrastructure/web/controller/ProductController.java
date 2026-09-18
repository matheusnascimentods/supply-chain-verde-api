package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.product.*;
import br.com.anhembi.supplychainverde.application.usecase.product.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
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
    @Operation(summary = "Listar produtos", description = "Retorna todos os produtos cadastrados.")
    public List<ProductResponseDTO> list() { return list.execute(); }
}
