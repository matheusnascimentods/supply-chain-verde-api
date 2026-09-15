package br.com.anhembi.supplychainverde.domain.entity;

import br.com.anhembi.supplychainverde.domain.enums.StageType;
import lombok.*;

import java.time.LocalDateTime;

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
}
