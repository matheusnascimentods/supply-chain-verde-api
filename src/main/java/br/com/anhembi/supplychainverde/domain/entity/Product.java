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
    /** RN-16: o produto é de um fornecedor só, o único que o cadastra e edita. */
    private Long supplierId;
    private String name;
    private ProductCategory category;
    private ProductUnit unit;
    private String description;
}
