package br.com.anhembi.supplychainverde.application.usecase.supplier;

import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierResponseDTO;
import br.com.anhembi.supplychainverde.application.mapper.SupplierDtoMapper;
import br.com.anhembi.supplychainverde.domain.exception.SupplierNotFoundException;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetSupplierUseCase {
    private final SupplierRepository supplierRepository;
    private final SupplierDtoMapper mapper;

    public SupplierResponseDTO execute(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new SupplierNotFoundException(supplierId));
    }
}
