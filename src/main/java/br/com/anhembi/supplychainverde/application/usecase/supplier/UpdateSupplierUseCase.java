package br.com.anhembi.supplychainverde.application.usecase.supplier;

import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.application.mapper.AddressDtoMapper;
import br.com.anhembi.supplychainverde.application.mapper.SupplierDtoMapper;
import br.com.anhembi.supplychainverde.domain.entity.Address;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.exception.SupplierNotFoundException;
import br.com.anhembi.supplychainverde.domain.repository.AddressRepository;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import br.com.anhembi.supplychainverde.domain.valueobject.Cnpj;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateSupplierUseCase {
    private final SupplierRepository supplierRepository;
    private final AddressRepository addressRepository;
    private final SupplierDtoMapper mapper;
    private final AddressDtoMapper addressMapper;

    @Transactional
    public SupplierResponseDTO execute(Long supplierId, SupplierRequestDTO request) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new SupplierNotFoundException(supplierId));
        if (request == null) throw new ValidationException("Requisição de fornecedor é obrigatória.");

        supplier.setName(request.name());
        supplier.setCnpj(new Cnpj(request.cnpj()));
        supplier.setPhone(request.phone());

        if (request.address() != null) {
            Address address = supplier.getAddress() != null ? supplier.getAddress() : new Address();
            addressMapper.update(request.address(), address);
            supplier.setAddress(addressRepository.save(address));
        }

        return mapper.toResponse(supplierRepository.save(supplier));
    }
}
