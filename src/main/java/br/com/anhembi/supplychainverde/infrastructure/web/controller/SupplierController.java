package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierRankingDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierResponseDTO;
import br.com.anhembi.supplychainverde.application.usecase.supplier.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {
    private final RegisterSupplierUseCase register;
    private final UpdateSupplierUseCase update;
    private final GetSupplierUseCase get;
    private final ListSuppliersUseCase list;
    private final RankSuppliersBySustainabilityUseCase rank;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponseDTO create(@RequestBody SupplierRequestDTO request) { return register.execute(request); }

    @PutMapping("/{supplierId}")
    public SupplierResponseDTO update(@PathVariable Long supplierId, @RequestBody SupplierRequestDTO request) { return update.execute(supplierId, request); }

    @GetMapping("/{supplierId}")
    public SupplierResponseDTO get(@PathVariable Long supplierId) { return get.execute(supplierId); }

    @GetMapping
    public List<SupplierResponseDTO> list() { return list.execute(); }

    @GetMapping("/ranking")
    public List<SupplierRankingDTO> ranking() { return rank.execute(); }
}
