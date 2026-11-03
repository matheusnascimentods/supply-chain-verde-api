package br.com.anhembi.supplychainverde.application.usecase.supplier;

import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierResponseDTO;
import br.com.anhembi.supplychainverde.application.mapper.SupplierDtoMapper;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListSuppliersUseCase {
    private final CompanyRepository supplierRepository;
    private final SupplierDtoMapper mapper;

    public List<SupplierResponseDTO> execute() {
        return supplierRepository.findAllSuppliers().stream().map(mapper::toResponse).toList();
    }
}
