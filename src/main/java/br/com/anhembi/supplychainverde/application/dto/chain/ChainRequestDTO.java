package br.com.anhembi.supplychainverde.application.dto.chain;
import br.com.anhembi.supplychainverde.application.dto.address.AddressRequestDTO;
import br.com.anhembi.supplychainverde.domain.enums.StageType;
/**
 * destination é obrigatório de PRODUCTION a DISTRIBUTION e proibido no RETAIL, que termina no mercado (RN-01, RN-03 da 018).
 * Não há datas: a etapa começa ao ser registrada e termina quando a próxima é registrada.
 */
public record ChainRequestDTO(Long batchId, AddressRequestDTO destination, StageType stageType) {}
