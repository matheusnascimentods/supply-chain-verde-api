package br.com.anhembi.supplychainverde.application.usecase.supplier;

import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.UpdateOwnSupplierRequestDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.entity.Address;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.exception.SupplierNotFoundException;
import br.com.anhembi.supplychainverde.domain.repository.AddressRepository;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.domain.service.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateOwnSupplierUseCase {
    private final SupplierRepository supplierRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final GetSupplierUseCase getSupplier;

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
        if (isBlankWhenSent(request.userName())) throw new ValidationException("Nome do usuário não pode ser vazio.");
        if (request.newPassword() != null) {
            if (request.newPassword().isBlank()) throw new ValidationException("Nova senha não pode ser vazia.");
            if (request.currentPassword() == null || !passwordHasher.matches(request.currentPassword(), user.getPassword())) {
                throw new ValidationException("Senha atual incorreta.");
            }
        }

        if (request.name() != null) supplier.setName(request.name());
        if (request.phone() != null) supplier.setPhone(request.phone());
        if (request.address() != null) {
            Address address = supplier.getAddress() != null ? supplier.getAddress() : new Address();
            address.setStreet(request.address().street());
            address.setNumber(request.address().number());
            address.setNeighborhood(request.address().neighborhood());
            address.setComplement(request.address().complement());
            address.setZipCode(request.address().zipCode());
            address.setCity(request.address().city());
            address.setState(request.address().state());
            supplier.setAddress(addressRepository.save(address));
        }
        supplierRepository.save(supplier);

        if (request.userName() != null || request.newPassword() != null) {
            if (request.userName() != null) user.setName(request.userName());
            if (request.newPassword() != null) user.setPassword(passwordHasher.hash(request.newPassword()));
            userRepository.save(user);
        }
        return getSupplier.execute(supplierId);
    }

    private boolean isBlankWhenSent(String value) {
        return value != null && value.isBlank();
    }
}
