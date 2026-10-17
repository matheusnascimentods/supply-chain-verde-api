package br.com.anhembi.supplychainverde.domain.enums;

/** Etapas da cadeia na ordem obrigatória (V39 garante a mesma sequência no banco). */
public enum StageType {
    PRODUCTION, STORAGE, PROCESSING, TRANSPORT, DISTRIBUTION, RETAIL;

    /** Próxima etapa da sequência; null depois de RETAIL. */
    public StageType next() {
        StageType[] stages = values();
        return ordinal() + 1 < stages.length ? stages[ordinal() + 1] : null;
    }
}
