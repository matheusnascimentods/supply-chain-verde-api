package br.com.anhembi.supplychainverde.application.dto.batch;
import java.math.BigDecimal;
/** RF-08: o mercado vem do token; produced_at nasce nulo e a etapa PRODUCTION preenche (RN-20). */
public record BatchRequestDTO(Long productId, Long supplierId, BigDecimal quantity) {}
