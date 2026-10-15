package br.com.anhembi.supplychainverde.application.usecase.supplier;

import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.user.UserRequestDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.application.mapper.SupplierDtoMapper;
import br.com.anhembi.supplychainverde.application.usecase.user.RegisterUserUseCase;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.repository.AddressRepository;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterSupplierUseCase {
    private final SupplierRepository supplierRepository;
    private final AddressRepository addressRepository;
    private final RegisterUserUseCase registerUser;
    private final SupplierDtoMapper mapper;

    @Transactional
    public SupplierResponseDTO execute(SupplierRequestDTO request) {
        if (request == null) throw new ValidationException("Requisição de fornecedor é obrigatória.");
        if (request.address() == null) throw new ValidationException("Endereço do fornecedor é obrigatório.");

        Supplier supplier = mapper.toDomain(request);
        supplier.setAddress(addressRepository.save(supplier.getAddress()));

        Supplier savedSupplier = supplierRepository.save(supplier);
        if (request.user() != null) {
            // Mesma transação: se o usuário falhar (ex.: email duplicado), o fornecedor também é desfeito (RN-04).
            Long userId = registerUser.execute(new UserRequestDTO(
                    request.user().name(), request.user().email(), request.user().password(),
                    UserRole.SUPPLIER, savedSupplier.getSupplierId())).userId();
            savedSupplier.setUserId(userId);
        }
        return mapper.toResponse(savedSupplier);
    }
}
