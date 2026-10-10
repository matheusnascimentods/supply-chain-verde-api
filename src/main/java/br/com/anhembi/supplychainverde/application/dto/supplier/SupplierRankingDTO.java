package br.com.anhembi.supplychainverde.application.dto.supplier;

import br.com.anhembi.supplychainverde.application.dto.address.AddressResponseDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SupplierRankingDTO(
        Long supplierId,
        String name,
        String cnpj,
        AddressResponseDTO address,
        String phone,
        LocalDate registeredAt,
        BigDecimal sustainabilityScore,
        Integer activeCertificationCount,
        BigDecimal totalCo2Kg,
        long reportCount,
        List<SupplierCertificationDTO> certifications,
        BigDecimal co2KgPerUnit
) {}
