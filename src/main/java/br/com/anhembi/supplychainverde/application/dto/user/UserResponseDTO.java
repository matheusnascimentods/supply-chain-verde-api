package br.com.anhembi.supplychainverde.application.dto.user;
import java.time.LocalDateTime;
import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
/** supplierId e supplierName ficam por compatibilidade (RF-04): iguais aos da companhia quando ela é fornecedora. */
public record UserResponseDTO(Long userId, String name, String email, UserRole role, LocalDateTime createdAt, Long supplierId,
        String supplierName, Long companyId, CompanyType companyType, String companyName) {}
