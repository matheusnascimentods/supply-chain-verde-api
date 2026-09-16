package br.com.anhembi.supplychainverde.application.dto.carbonemission;
import br.com.anhembi.supplychainverde.domain.enums.CalculationMethod;
public record CarbonEmissionRequestDTO(Long chainId, CalculationMethod calculationMethod) {}
