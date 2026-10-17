package br.com.anhembi.supplychainverde.domain.enums;

/** Etapas da cadeia na ordem obrigatória (V39 garante a mesma sequência no banco). */
public enum StageType {
    PRODUCTION, STORAGE, PROCESSING, TRANSPORT, DISTRIBUTION, RETAIL;

    /** Próxima etapa da sequência; null depois de RETAIL. Espelha o CASE de previous_stage_type na V39. */
    public StageType next() {
        return switch (this) {
            case PRODUCTION -> STORAGE;
            case STORAGE -> PROCESSING;
            case PROCESSING -> TRANSPORT;
            case TRANSPORT -> DISTRIBUTION;
            case DISTRIBUTION -> RETAIL;
            case RETAIL -> null;
        };
    }
}
