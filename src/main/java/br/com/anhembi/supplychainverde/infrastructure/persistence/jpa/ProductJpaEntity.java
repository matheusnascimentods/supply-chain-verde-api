package br.com.anhembi.supplychainverde.infrastructure.persistence.jpa;

import br.com.anhembi.supplychainverde.domain.enums.ProductCategory;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false)
    private ProductUnit unit;

    @Column(name = "description")
    private String description;
}
