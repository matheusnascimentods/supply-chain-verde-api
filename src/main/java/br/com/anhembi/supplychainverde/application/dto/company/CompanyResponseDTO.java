package br.com.anhembi.supplychainverde.application.dto.company;

import br.com.anhembi.supplychainverde.application.dto.address.AddressResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.plan.PlanResponseDTO;
import br.com.anhembi.supplychainverde.domain.enums.CompanyType;

import java.time.LocalDate;

public record CompanyResponseDTO(Long companyId, String name, String cnpj, String email, String phone, CompanyType type,
        AddressResponseDTO address, LocalDate registeredAt, PlanResponseDTO plan) {}
