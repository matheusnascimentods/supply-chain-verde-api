package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierRankingDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierResponseDTO;
import br.com.anhembi.supplychainverde.application.usecase.supplier.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@Tag(name = "Fornecedores", description = "Cadastro, consulta e ranking de fornecedores.")
public class SupplierController {
    private final RegisterSupplierUseCase register;
    private final UpdateSupplierUseCase update;
    private final GetSupplierUseCase get;
    private final ListSuppliersUseCase list;
    private final RankSuppliersBySustainabilityUseCase rank;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar fornecedor", description = "Cadastra um fornecedor e seu endereço.")
    public SupplierResponseDTO create(@RequestBody SupplierRequestDTO request) { return register.execute(request); }

    @PutMapping("/{supplierId}")
    @Operation(summary = "Atualizar fornecedor", description = "Atualiza os dados cadastrais de um fornecedor.")
    public SupplierResponseDTO update(@PathVariable Long supplierId, @RequestBody SupplierRequestDTO request) { return update.execute(supplierId, request); }

    @GetMapping("/{supplierId}")
    @Operation(summary = "Consultar fornecedor", description = "Busca um fornecedor pelo identificador.")
    public SupplierResponseDTO get(@PathVariable Long supplierId) { return get.execute(supplierId); }

    @GetMapping
    @Operation(summary = "Listar fornecedores", description = "Retorna todos os fornecedores cadastrados.")
    public List<SupplierResponseDTO> list() { return list.execute(); }

    @GetMapping("/ranking")
    @Operation(summary = "Ranquear fornecedores", description = "Retorna fornecedores ordenados pelo desempenho de sustentabilidade.")
    public List<SupplierRankingDTO> ranking() { return rank.execute(); }
}
