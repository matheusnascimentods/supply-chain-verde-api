package br.com.anhembi.supplychainverde.domain.enums;

public enum UserRole {
    ADMIN, AUDITOR, SUPPLIER, MANAGER, PLATFORM_ADMIN;

    /** RN-07. O PLATFORM_ADMIN não pertence a companhia e só nasce por migration (RN-10). */
    public boolean allowedFor(CompanyType type) {
        return switch (this) {
            case ADMIN -> type != null;
            case MANAGER, AUDITOR -> type == CompanyType.MARKET;
            case SUPPLIER -> type == CompanyType.SUPPLIER;
            case PLATFORM_ADMIN -> false;
        };
    }
}
