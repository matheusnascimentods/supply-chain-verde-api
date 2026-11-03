package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.dto.user.UserResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.pagination.OffsetPageResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.application.mapper.UserDtoMapper;
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
    private final CompanyRepository supplierRepository;
    private final UserDtoMapper mapper;

    public OffsetPageResponseDTO<UserResponseDTO> execute(String email, UserRole role, int limit, int offset) {
        List<User> users = userRepository.findPage(email, role, limit, offset);
        long totalElements = userRepository.countPage(email, role);
        Map<Long, Company> suppliersByUserId = supplierRepository
                .findByUserIdIn(users.stream().map(User::getUserId).toList()).stream()
                .collect(Collectors.toMap(Company::getUserId, Function.identity()));
        List<UserResponseDTO> items = users.stream()
                .map(user -> mapper.toResponse(user, suppliersByUserId.get(user.getUserId())))
                .toList();
        return OffsetPageResponseDTO.of(items, limit, offset, totalElements);
    }
}
