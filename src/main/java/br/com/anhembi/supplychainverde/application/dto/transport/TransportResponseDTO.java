package br.com.anhembi.supplychainverde.application.dto.transport;
import java.math.BigDecimal;
import br.com.anhembi.supplychainverde.domain.enums.FuelType;
import br.com.anhembi.supplychainverde.domain.enums.TransportMode;
public record TransportResponseDTO(Long transportId, Long chainId, TransportMode transportMode, BigDecimal distance, FuelType fuelType, BigDecimal capacity) {}
