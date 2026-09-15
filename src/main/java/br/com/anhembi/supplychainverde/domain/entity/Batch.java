package br.com.anhembi.supplychainverde.domain.entity;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Batch {
    private Long batchId;
    private Product product;
    private Supplier supplier;
    private BigDecimal quantity;
    private LocalDate producedAt;
}
