package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.product.*;
import br.com.anhembi.supplychainverde.application.usecase.product.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final RegisterProductUseCase register;
    private final UpdateProductUseCase update;
    private final ListProductsUseCase list;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponseDTO create(@RequestBody ProductRequestDTO request) { return register.execute(request); }

    @PutMapping("/{productId}")
    public ProductResponseDTO update(@PathVariable Long productId, @RequestBody ProductRequestDTO request) { return update.execute(productId, request); }

    @GetMapping
    public List<ProductResponseDTO> list() { return list.execute(); }
}
