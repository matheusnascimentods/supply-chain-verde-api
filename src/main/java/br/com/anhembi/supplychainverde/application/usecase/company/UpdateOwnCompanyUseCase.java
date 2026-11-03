package br.com.anhembi.supplychainverde.application.usecase.company;

import br.com.anhembi.supplychainverde.application.dto.company.CompanyResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.company.UpdateOwnCompanyRequestDTO;
import br.com.anhembi.supplychainverde.application.exception.CompanyNotFoundException;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.application.mapper.AddressDtoMapper;
import br.com.anhembi.supplychainverde.domain.entity.Address;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.repository.AddressRepository;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateOwnCompanyUseCase {
    private final CompanyRepository companyRepository;
    private final AddressRepository addressRepository;
    private final AddressDtoMapper addressMapper;
    private final GetOwnCompanyUseCase getOwnCompany;

    @Transactional
    public CompanyResponseDTO execute(Long userId, UpdateOwnCompanyRequestDTO request) {
        if (request == null) throw new ValidationException("Requisição de atualização é obrigatória.");
        if (request.name() != null && request.name().isBlank()) throw new ValidationException("Razão social não pode ser vazia.");
        if (request.email() != null && !CompanyEmails.isValid(request.email())) throw new ValidationException("Email da companhia inválido.");
        Long companyId = getOwnCompany.companyIdOf(userId);
        Company company = companyRepository.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));

        if (request.name() != null) company.setName(request.name());
        if (request.email() != null) company.setEmail(request.email());
        if (request.phone() != null) company.setPhone(request.phone());
        if (request.address() != null) {
            Address address = company.getAddress() != null ? company.getAddress() : new Address();
            addressMapper.update(request.address(), address);
            company.setAddress(addressRepository.save(address));
        }
        companyRepository.save(company);
        return getOwnCompany.execute(userId);
    }
}
