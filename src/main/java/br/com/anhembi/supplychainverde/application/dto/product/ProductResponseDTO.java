package br.com.anhembi.supplychainverde.application.dto.product;
import br.com.anhembi.supplychainverde.domain.enums.ProductCategory;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;

import java.math.BigDecimal;

/** supplierName, co2KgPerUnit e acceptedBatchesLast30Days vêm só no catálogo do mercado (RN-01); nulos para o fornecedor (RN-07). */
public record ProductResponseDTO(Long productId, Long supplierId, String name, ProductCategory category, ProductUnit unit,
                                 String description, String supplierName, BigDecimal co2KgPerUnit,
                                 Long acceptedBatchesLast30Days) {}
