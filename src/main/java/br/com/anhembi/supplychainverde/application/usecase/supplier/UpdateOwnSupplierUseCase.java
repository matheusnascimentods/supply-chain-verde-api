package br.com.anhembi.supplychainverde.application.usecase.supplier;

import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.UpdateOwnSupplierRequestDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.application.mapper.AddressDtoMapper;
import br.com.anhembi.supplychainverde.application.usecase.user.OwnAccountChanges;
import br.com.anhembi.supplychainverde.domain.entity.Address;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.exception.SupplierNotFoundException;
import br.com.anhembi.supplychainverde.domain.repository.AddressRepository;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateOwnSupplierUseCase {
    private final SupplierRepository supplierRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final OwnAccountChanges ownAccountChanges;
    private final GetSupplierUseCase getSupplier;
    private final AddressDtoMapper addressMapper;

    @Transactional
    public SupplierResponseDTO execute(Long supplierId, UpdateOwnSupplierRequestDTO request) {
        if (request == null) throw new ValidationException("Requisição de atualização é obrigatória.");
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new SupplierNotFoundException(supplierId));
        // O usuário vem do vínculo, não do token: quem edita é sempre o usuário ligado a este fornecedor.
        User user = userRepository.findById(supplier.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + supplier.getUserId()));

        // Valida tudo antes de gravar qualquer coisa.
        if (isBlankWhenSent(request.name())) throw new ValidationException("Razão social não pode ser vazia.");
        ownAccountChanges.validate(user, request.userName(), request.currentPassword(), request.newPassword());

        if (request.name() != null) supplier.setName(request.name());
        if (request.phone() != null) supplier.setPhone(request.phone());
        if (request.address() != null) {
            Address address = supplier.getAddress() != null ? supplier.getAddress() : new Address();
            addressMapper.update(request.address(), address);
            supplier.setAddress(addressRepository.save(address));
        }
        supplierRepository.save(supplier);

        if (ownAccountChanges.apply(user, request.userName(), request.newPassword())) {
            userRepository.save(user);
        }
        return getSupplier.execute(supplierId);
    }

    private boolean isBlankWhenSent(String value) {
        return value != null && value.isBlank();
    }
}
