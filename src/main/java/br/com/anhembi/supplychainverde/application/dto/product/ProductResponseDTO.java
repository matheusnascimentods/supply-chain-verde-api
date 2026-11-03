package br.com.anhembi.supplychainverde.application.dto.product;
import br.com.anhembi.supplychainverde.domain.enums.ProductCategory;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;
public record ProductResponseDTO(Long productId, Long supplierId, String name, ProductCategory category, ProductUnit unit, String description) {}
