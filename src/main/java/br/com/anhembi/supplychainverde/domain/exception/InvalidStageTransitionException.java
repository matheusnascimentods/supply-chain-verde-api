package br.com.anhembi.supplychainverde.domain.exception;

import br.com.anhembi.supplychainverde.domain.enums.StageType;

public class InvalidStageTransitionException extends DomainException {
    public InvalidStageTransitionException(StageType currentStage, StageType nextStage) {
        super(currentStage == null
                ? "Transição de etapa inválida: o lote ainda não tem etapas e deve começar em PRODUCTION, não em " + nextStage + "."
                : "Transição de etapa inválida: " + currentStage + " -> " + nextStage + ".");
    }
}
