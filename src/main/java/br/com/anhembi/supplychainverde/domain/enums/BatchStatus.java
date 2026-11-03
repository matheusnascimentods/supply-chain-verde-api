package br.com.anhembi.supplychainverde.domain.enums;

/** RN-18 e RN-19: o lote nasce PENDING; a primeira etapa o ativa e a recusa é final. */
public enum BatchStatus {
    PENDING, ACTIVE, REJECTED
}
