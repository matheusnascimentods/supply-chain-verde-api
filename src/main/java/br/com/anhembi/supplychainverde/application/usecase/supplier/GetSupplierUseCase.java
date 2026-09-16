package br.com.anhembi.supplychainverde.application.usecase.supplier;

import br.com.anhembi.supplychainverde.application.dto.address.AddressResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Address;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.exception.SupplierNotFoundException;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetSupplierUseCase {
    private final SupplierRepository supplierRepository;

    public SupplierResponseDTO execute(Long supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new SupplierNotFoundException(supplierId));
        return toResponse(supplier);
    }

    public SupplierResponseDTO getById(Long supplierId) {
        return execute(supplierId);
    }

    private SupplierResponseDTO toResponse(Supplier supplier) {
        return new SupplierResponseDTO(
                supplier.getSupplierId(),
                supplier.getName(),
                supplier.getCnpj() != null ? supplier.getCnpj().value() : null,
                toAddressResponse(supplier.getAddress()),
                supplier.getPhone(),
                supplier.getRegisteredAt()
        );
    }

    private AddressResponseDTO toAddressResponse(Address address) {
        if (address == null) return null;
        return new AddressResponseDTO(
                address.getAddressId(),
                address.getStreet(),
                address.getNumber(),
                address.getNeighborhood(),
                address.getComplement(),
                address.getZipCode(),
                address.getCity(),
                address.getState()
        );
    }
}
