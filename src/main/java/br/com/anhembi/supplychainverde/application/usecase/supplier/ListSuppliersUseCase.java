package br.com.anhembi.supplychainverde.application.usecase.supplier;

import br.com.anhembi.supplychainverde.application.dto.address.AddressResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Address;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListSuppliersUseCase {
    private final SupplierRepository supplierRepository;

    public List<SupplierResponseDTO> execute() {
        return supplierRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<SupplierResponseDTO> list() {
        return execute();
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
