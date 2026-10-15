package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.dto.user.UserRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.user.UserResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.ConflictException;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.exception.SupplierNotFoundException;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.domain.service.PasswordHasher;
import br.com.anhembi.supplychainverde.application.mapper.UserDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {
    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;
    private final PasswordHasher passwordHasher;
    private final UserDtoMapper mapper;

    @Transactional
    public UserResponseDTO execute(UserRequestDTO request) {
        if (request == null) throw new ValidationException("Dados do usuário são obrigatórios.");
        if (isBlank(request.name()) || isBlank(request.email()) || isBlank(request.password())) {
            throw new ValidationException("Nome, email e senha do usuário são obrigatórios.");
        }
        if (request.role() == null) throw new ValidationException("Perfil do usuário é obrigatório.");
        boolean supplierUser = request.role() == UserRole.SUPPLIER;
        if (supplierUser && request.supplierId() == null) {
            throw new ValidationException("supplierId é obrigatório para usuários SUPPLIER.");
        }
        if (!supplierUser && request.supplierId() != null) {
            throw new ValidationException("supplierId só pode ser informado para usuários SUPPLIER.");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email já cadastrado: " + request.email());
        }

        Supplier supplier = null;
        if (supplierUser) {
            supplier = supplierRepository.findById(request.supplierId())
                    .orElseThrow(() -> new SupplierNotFoundException(request.supplierId()));
            if (supplier.getUserId() != null) {
                throw new ConflictException("Fornecedor já possui usuário: " + request.supplierId());
            }
        }

        User saved = userRepository.save(User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordHasher.hash(request.password()))
                .role(request.role())
                .createdAt(LocalDateTime.now())
                .build());
        if (supplier != null) {
            supplier.setUserId(saved.getUserId());
            supplierRepository.save(supplier);
        }
        return mapper.toResponse(saved, supplier);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
