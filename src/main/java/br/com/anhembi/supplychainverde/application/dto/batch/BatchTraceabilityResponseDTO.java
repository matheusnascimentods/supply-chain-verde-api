package br.com.anhembi.supplychainverde.application.dto.batch;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import br.com.anhembi.supplychainverde.application.dto.chain.ChainResponseDTO;
public record BatchTraceabilityResponseDTO(Long batchId, String productName, String supplierName, BigDecimal quantity, LocalDate producedAt, List<ChainResponseDTO> stages, BigDecimal totalCo2Kg) {}
