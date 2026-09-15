package br.com.anhembi.supplychainverde.domain.exception;

import br.com.anhembi.supplychainverde.domain.enums.StageType;

public class InvalidStageTransitionException extends DomainException {
    public InvalidStageTransitionException(StageType currentStage, StageType nextStage) {
        super("Transição de etapa inválida: " + currentStage + " -> " + nextStage);
    }
}
