package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.dto.user.UserResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.pagination.OffsetPageResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListUsersUseCase {
    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;

    public OffsetPageResponseDTO<UserResponseDTO> execute(String email, UserRole role, int limit, int offset) {
        List<User> users = userRepository.findPage(email, role, limit, offset);
        long totalElements = userRepository.countPage(email, role);
        Map<Long, Supplier> suppliersByUserId = supplierRepository
                .findByUserIdIn(users.stream().map(User::getUserId).toList()).stream()
                .collect(Collectors.toMap(Supplier::getUserId, Function.identity()));
        List<UserResponseDTO> items = users.stream()
                .map(user -> {
                    Supplier supplier = suppliersByUserId.get(user.getUserId());
                    return new UserResponseDTO(
                            user.getUserId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt(),
                            supplier == null ? null : supplier.getSupplierId(),
                            supplier == null ? null : supplier.getName());
                })
                .toList();
        return OffsetPageResponseDTO.of(items, limit, offset, totalElements);
    }
}
