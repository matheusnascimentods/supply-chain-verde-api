package br.com.anhembi.supplychainverde.application.dto.product;
import br.com.anhembi.supplychainverde.domain.enums.ProductCategory;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;
public record ProductRequestDTO(String name, ProductCategory category, ProductUnit unit, String description) {}
