package br.com.anhembi.supplychainverde.application.usecase.supplier;

import br.com.anhembi.supplychainverde.application.dto.address.AddressResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.entity.Address;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.exception.SupplierNotFoundException;
import br.com.anhembi.supplychainverde.domain.repository.AddressRepository;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import br.com.anhembi.supplychainverde.domain.valueobject.Cnpj;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateSupplierUseCase {
    private final SupplierRepository supplierRepository;
    private final AddressRepository addressRepository;

    public SupplierResponseDTO execute(Long supplierId, SupplierRequestDTO request) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new SupplierNotFoundException(supplierId));
        if (request == null) throw new ValidationException("Requisição de fornecedor é obrigatória.");

        supplier.setName(request.name());
        supplier.setCnpj(new Cnpj(request.cnpj()));
        supplier.setPhone(request.phone());

        if (request.address() != null) {
            Address current = supplier.getAddress();
            Address address = current != null ? current : new Address();
            address.setStreet(request.address().street());
            address.setNumber(request.address().number());
            address.setNeighborhood(request.address().neighborhood());
            address.setComplement(request.address().complement());
            address.setZipCode(request.address().zipCode());
            address.setCity(request.address().city());
            address.setState(request.address().state());
            supplier.setAddress(addressRepository.save(address));
        }

        Supplier updated = supplierRepository.save(supplier);
        return toResponse(updated);
    }

    public SupplierResponseDTO update(Long supplierId, SupplierRequestDTO request) {
        return execute(supplierId, request);
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
