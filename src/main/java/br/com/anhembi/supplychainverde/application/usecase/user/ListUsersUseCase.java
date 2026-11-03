package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.dto.pagination.OffsetPageResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.user.UserResponseDTO;
import br.com.anhembi.supplychainverde.application.mapper.UserDtoMapper;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListUsersUseCase {
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final UserDtoMapper mapper;

    /** RN-13b: companyId nulo (PLATFORM_ADMIN) lista todos os usuários. */
    public OffsetPageResponseDTO<UserResponseDTO> execute(String email, Long companyId, int limit, int offset) {
        List<User> users = userRepository.findPage(email, companyId, limit, offset);
        long totalElements = userRepository.countPage(email, companyId);
        Map<Long, Company> companies = companyRepository
                .findAllById(users.stream().map(User::getCompanyId).filter(Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(Company::getCompanyId, Function.identity()));
        List<UserResponseDTO> items = users.stream()
                .map(user -> mapper.toResponse(user, companies.get(user.getCompanyId())))
                .toList();
        return OffsetPageResponseDTO.of(items, limit, offset, totalElements);
    }
}
