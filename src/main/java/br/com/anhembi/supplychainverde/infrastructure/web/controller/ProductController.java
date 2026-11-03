package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.product.*;
import br.com.anhembi.supplychainverde.application.dto.pagination.OffsetPageResponseDTO;
import br.com.anhembi.supplychainverde.application.usecase.product.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @Operation(summary = "Cadastrar produto", description = "Somente a companhia fornecedora (SUPPLIER ou ADMIN). O produto nasce no fornecedor do usuário (RN-16); outras companhias recebem 403.")
    public ProductResponseDTO create(@RequestBody ProductRequestDTO request, @AuthenticationPrincipal CustomUserPrincipal principal) {
        return register.execute(request, principal.supplierId());
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Atualizar produto", description = "Somente o fornecedor dono do produto; os demais recebem 403.")
    public ProductResponseDTO update(@PathVariable Long productId, @RequestBody ProductRequestDTO request,
                                     @AuthenticationPrincipal CustomUserPrincipal principal) {
        return update.execute(productId, request, principal.supplierId());
    }

    @GetMapping
    @Operation(summary = "Listar produtos", description = "Busca e pagina produtos por nome, descrição ou categoria. A companhia fornecedora vê só os próprios; o mercado vê os de todos os fornecedores (catálogo) e filtra por supplierId, usado no modal de lote.")
    public OffsetPageResponseDTO<ProductResponseDTO> list(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @Min(1) Long supplierId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return list.execute(limit, offset, search, principal.isSupplierCompany() ? principal.supplierId() : supplierId);
    }
}
