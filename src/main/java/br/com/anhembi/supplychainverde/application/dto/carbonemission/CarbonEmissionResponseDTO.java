package br.com.anhembi.supplychainverde.application.dto.carbonemission;
import java.math.BigDecimal;
import java.time.LocalDate;
import br.com.anhembi.supplychainverde.domain.enums.CalculationMethod;
import br.com.anhembi.supplychainverde.domain.enums.EnergySource;
public record CarbonEmissionResponseDTO(Long emissionId, Long chainId, BigDecimal emissionFactor, BigDecimal co2Kg,
                                        CalculationMethod calculationMethod, LocalDate calculatedAt,
                                        BigDecimal cargoWeightKg, EnergySource energySource, BigDecimal consumption) {}
