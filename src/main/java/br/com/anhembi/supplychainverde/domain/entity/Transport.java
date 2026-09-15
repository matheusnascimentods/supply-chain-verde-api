package br.com.anhembi.supplychainverde.domain.entity;

import br.com.anhembi.supplychainverde.domain.enums.FuelType;
import br.com.anhembi.supplychainverde.domain.enums.TransportMode;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transport {
    private Long transportId;
    private Chain chain;
    private TransportMode transportMode;
    private BigDecimal distance;
    private FuelType fuelType;
    private BigDecimal capacity;
}
