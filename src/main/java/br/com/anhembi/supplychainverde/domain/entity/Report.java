package br.com.anhembi.supplychainverde.domain.entity;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {
    private Long reportId;
    private Supplier supplier;
    private LocalDate periodStartAt;
    private LocalDate periodEndAt;
    private BigDecimal totalCo2Kg;
    private Integer trackedProductCount;
    private LocalDateTime generatedAt;

}
