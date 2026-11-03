package br.com.anhembi.supplychainverde.application.usecase.company;

import br.com.anhembi.supplychainverde.application.dto.company.CompanyResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.plan.PlanResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.CompanyNotFoundException;
import br.com.anhembi.supplychainverde.application.mapper.AddressDtoMapper;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.repository.PlanRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetOwnCompanyUseCase {
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PlanRepository planRepository;
    private final AddressDtoMapper addressMapper;

    @Transactional(readOnly = true)
    public CompanyResponseDTO execute(Long userId) {
        Long companyId = companyIdOf(userId);
        Company company = companyRepository.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
        return new CompanyResponseDTO(company.getCompanyId(), company.getName(), company.getCnpj().value(),
                company.getEmail(), company.getPhone(), company.getType(), addressMapper.toResponse(company.getAddress()),
                company.getRegisteredAt(), PlanResponseDTO.of(planRepository.findActiveByCompanyId(companyId).orElse(null)));
    }

    /** A companhia vem do usuário, não do token. O PLATFORM_ADMIN não tem companhia (RN-06): 403. */
    Long companyIdOf(Long userId) {
        Long companyId = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + userId))
                .getCompanyId();
        if (companyId == null) throw new AccessDeniedException("Usuário sem companhia.");
        return companyId;
    }
}
