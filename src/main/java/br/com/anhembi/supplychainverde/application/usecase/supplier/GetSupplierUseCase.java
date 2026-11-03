package br.com.anhembi.supplychainverde.application.usecase.supplier;

import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierResponseDTO;
import br.com.anhembi.supplychainverde.application.mapper.SupplierDtoMapper;
import br.com.anhembi.supplychainverde.application.exception.CompanyNotFoundException;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetSupplierUseCase {
    private final CompanyRepository supplierRepository;
    private final SupplierDtoMapper mapper;

    public SupplierResponseDTO execute(Long supplierId) {
        return supplierRepository.findSupplierById(supplierId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new CompanyNotFoundException(supplierId));
    }
}
