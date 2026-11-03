package br.com.anhembi.supplychainverde.application.usecase.company;

import br.com.anhembi.supplychainverde.application.dto.auth.LoginResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.company.RegisterCompanyRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.company.RegisterCompanyRequestDTO.AdminData;
import br.com.anhembi.supplychainverde.application.dto.company.RegisterCompanyRequestDTO.CompanyData;
import br.com.anhembi.supplychainverde.application.exception.ConflictException;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.application.mapper.AddressDtoMapper;
import br.com.anhembi.supplychainverde.application.usecase.user.AuthenticateUserUseCase;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.entity.Plan;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.repository.AddressRepository;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.repository.PlanRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.domain.service.PasswordHasher;
import br.com.anhembi.supplychainverde.domain.valueobject.Cnpj;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** RF-02: endereço, companhia, assinatura e o ADMIN numa transação; a resposta é a mesma do login. */
@Service
@RequiredArgsConstructor
public class RegisterCompanyUseCase {
    private final CompanyRepository companyRepository;
    private final PlanRepository planRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AddressDtoMapper addressMapper;
    private final AuthenticateUserUseCase authenticate;

    @Transactional
    public LoginResponseDTO execute(RegisterCompanyRequestDTO request) {
        if (request == null || request.company() == null || request.admin() == null) {
            throw new ValidationException("Companhia e administrador são obrigatórios.");
        }
        if (request.type() == null) throw new ValidationException("Tipo da companhia é obrigatório.");
        CompanyData data = request.company();
        AdminData admin = request.admin();
        if (isBlank(data.name())) throw new ValidationException("Razão social é obrigatória.");
        if (data.address() == null) throw new ValidationException("Endereço da companhia é obrigatório.");
        if (!CompanyEmails.isValid(data.email())) throw new ValidationException("Email da companhia inválido.");
        if (isBlank(admin.name())) throw new ValidationException("Nome do administrador é obrigatório.");
        if (!CompanyEmails.isValid(admin.email())) throw new ValidationException("Email do administrador inválido.");
        if (isBlank(admin.password())) throw new ValidationException("Senha do administrador é obrigatória.");
        Cnpj cnpj = new Cnpj(data.cnpj() == null ? "" : data.cnpj());
        Plan plan = planRepository.findByCode(request.planCode())
                .orElseThrow(() -> new ValidationException("Plano inexistente: " + request.planCode()));
        if (companyRepository.existsByCnpj(cnpj)) throw new ConflictException("CNPJ já cadastrado: " + cnpj.value());
        if (userRepository.existsByEmail(admin.email())) throw new ConflictException("Email já cadastrado: " + admin.email());

        Company company = companyRepository.save(Company.builder()
                .name(data.name())
                .cnpj(cnpj)
                .email(data.email())
                .phone(data.phone())
                .type(request.type())
                .address(addressRepository.save(addressMapper.toDomain(data.address())))
                .build());
        companyRepository.subscribe(company.getCompanyId(), plan.planId());
        User user = userRepository.save(User.builder()
                .name(admin.name())
                .email(admin.email())
                .password(passwordHasher.hash(admin.password()))
                .role(UserRole.ADMIN)
                .companyId(company.getCompanyId())
                .createdAt(LocalDateTime.now())
                .build());
        return authenticate.issueToken(user);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
