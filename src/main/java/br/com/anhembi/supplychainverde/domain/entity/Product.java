package br.com.anhembi.supplychainverde.domain.entity;

import br.com.anhembi.supplychainverde.domain.enums.ProductCategory;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private Long productId;
    private String name;
    private ProductCategory category;
    private ProductUnit unit;
    private String description;
}
