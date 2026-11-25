package br.com.anhembi.supplychainverde.domain.entity;

import br.com.anhembi.supplychainverde.domain.enums.StageType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chain {
    private Long chainId;
    private Batch batch;
    private Address originAddress;      // nullable — ver pendência na seção 13
    private Address destinationAddress; // nullable — ver pendência na seção 13
    private User responsibleUser;
    private StageType stageType;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    /** RN-04 (018): a origem da PRODUCTION é o fornecedor; a das outras etapas é o destino da etapa anterior. */
    public static Map<Long, Address> origins(List<Chain> stagesOfOneBatch) {
        Map<Long, Address> origins = new HashMap<>(); // aceita null, que o Collectors.toMap recusaria
        Address previous = null;
        boolean first = true;
        for (Chain stage : stagesOfOneBatch.stream().sorted(Comparator.comparing(Chain::getStageType)).toList()) {
            origins.put(stage.getChainId(), first ? stage.getBatch().supplierAddress() : previous);
            previous = stage.getDestinationAddress();
            first = false;
        }
        return origins;
    }
}
