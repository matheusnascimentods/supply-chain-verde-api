package br.com.anhembi.supplychainverde.application.dto.report;

import br.com.anhembi.supplychainverde.application.dto.address.AddressResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierCertificationDTO;
import br.com.anhembi.supplychainverde.domain.enums.CalculationMethod;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;
import br.com.anhembi.supplychainverde.domain.enums.StageType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReportDetailDTO(
        Long reportId,
        Long supplierId,
        String supplierCnpj,
        String supplierName,
        LocalDate periodStartAt,
        LocalDate periodEndAt,
        BigDecimal totalCo2Kg,
        long totalBatchCount,
        Integer trackedProductCount,
        LocalDateTime generatedAt,
        Supplier supplier,
        Emissions emissions,
        List<SupplierCertificationDTO> certifications
) {
    public record Supplier(String phone, AddressResponseDTO address) {}

    public record Emissions(List<ByStage> byStage, List<ByBatch> byBatch, List<ByMethod> byMethod) {}

    public record ByStage(StageType stageType, BigDecimal co2Kg) {}

    public record ByBatch(Long batchId, String productName, BigDecimal quantity, ProductUnit unit, BigDecimal co2Kg) {}

    public record ByMethod(CalculationMethod calculationMethod, BigDecimal emissionFactor, BigDecimal co2Kg) {}
}
