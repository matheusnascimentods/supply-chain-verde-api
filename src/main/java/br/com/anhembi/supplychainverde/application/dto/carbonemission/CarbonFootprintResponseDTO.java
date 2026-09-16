package br.com.anhembi.supplychainverde.application.dto.carbonemission;
import java.math.BigDecimal;
import java.util.List;
public record CarbonFootprintResponseDTO(Long batchId, BigDecimal totalCo2Kg, List<CarbonEmissionResponseDTO> emissionsByStage) {}
