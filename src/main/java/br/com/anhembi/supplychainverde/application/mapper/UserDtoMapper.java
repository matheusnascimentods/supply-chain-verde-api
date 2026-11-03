package br.com.anhembi.supplychainverde.application.mapper;
import br.com.anhembi.supplychainverde.application.dto.user.UserResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserDtoMapper {
    default UserResponseDTO toResponse(User user, Company company) {
        boolean supplier = company != null && company.getType() == CompanyType.SUPPLIER;
        return new UserResponseDTO(user.getUserId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt(),
                supplier ? company.getCompanyId() : null, supplier ? company.getName() : null,
                company == null ? null : company.getCompanyId(),
                company == null ? null : company.getType(),
                company == null ? null : company.getName());
    }
}
