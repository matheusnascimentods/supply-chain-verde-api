package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.usecase.supplier.*;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@Validated
@Tag(name = "Fornecedores", description = "Consulta e ranking de fornecedores. O cadastro é por POST /companies.")
public class SupplierController {
    private final GetSupplierUseCase get;
    private final RankSuppliersBySustainabilityUseCase rank;

    @GetMapping
    @Operation(
            summary = "Consultar fornecedores",
            description = "Sem supplierId ou ranked=true, lista fornecedores; ranked=true retorna a mesma estrutura em ranking paginado. Cada item inclui supplierId, name, cnpj, address, phone, registeredAt, sustainabilityScore, activeCertificationCount, totalCo2Kg, reportCount e certifications. supplierId isolado retorna o detalhe cadastral. reportCount é uma contagem não negativa; certifications é sempre uma lista, vazia quando não houver certificações. O catálogo é global para o mercado (RN-13): cada item traz workedWith, que diz se o fornecedor tem ou teve lote com o mercado logado, e workedWith=true restringe a lista a eles (não combina com supplierId). reportCount conta só os relatórios do mercado logado. Para o PLATFORM_ADMIN, workedWith vem sempre false. Companhia fornecedora recebe 403, exceto no próprio detalhe (supplierId = a própria)."
    )
    public Object getSuppliers(
            @RequestParam(required = false) @Min(1) Long supplierId,
            @RequestParam(required = false) Boolean ranked,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean workedWith,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        // RN-13a: a companhia fornecedora só lê o próprio detalhe.
        if (principal.isSupplierCompany() && !principal.supplierId().equals(supplierId)) {
            throw new AccessDeniedException("Companhia fornecedora não acessa o catálogo de fornecedores.");
        }
        if (supplierId != null && ranked != null) {
            throw new IllegalArgumentException("supplierId e ranked são parâmetros mutuamente exclusivos.");
        }
        if (workedWith && supplierId != null) {
            throw new IllegalArgumentException("workedWith não combina com supplierId.");
        }
        if (supplierId != null) {
            return get.execute(supplierId);
        }
        if (Boolean.TRUE.equals(ranked)) {
            return rank.execute(limit, offset, search, workedWith, principal.marketId());
        }
        return rank.executeAll(workedWith, principal.marketId());
    }
}
