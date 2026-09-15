package br.com.anhembi.supplychainverde.domain.valueobject;

public record Cnpj(String value) {

    private static final int[] WEIGHTS_FIRST = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_SECOND = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    public Cnpj {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CNPJ não pode ser nulo ou vazio.");
        }

        value = value.replaceAll("\\D", "");

        if (value.length() != 14) {
            throw new IllegalArgumentException("CNPJ deve conter exatamente 14 dígitos.");
        }

        if (isAllSameDigits(value) || !hasValidCheckDigits(value)) {
            throw new IllegalArgumentException("CNPJ inválido: " + value);
        }
    }

    public String formatted() {
        return String.format("%s.%s.%s/%s-%s",
                value.substring(0, 2),
                value.substring(2, 5),
                value.substring(5, 8),
                value.substring(8, 12),
                value.substring(12, 14));
    }

    public static boolean isValid(String cnpj) {
        if (cnpj == null || cnpj.isBlank()) {
            return false;
        }
        String clean = cnpj.replaceAll("\\D", "");
        if (clean.length() != 14 || isAllSameDigits(clean)) {
            return false;
        }
        return hasValidCheckDigits(clean);
    }

    private static boolean isAllSameDigits(String cnpj) {
        char first = cnpj.charAt(0);
        for (int i = 1; i < cnpj.length(); i++) {
            if (cnpj.charAt(i) != first) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasValidCheckDigits(String cnpj) {
        int sum1 = 0;
        for (int i = 0; i < 12; i++) {
            sum1 += (cnpj.charAt(i) - '0') * WEIGHTS_FIRST[i];
        }
        int remainder1 = sum1 % 11;
        int digit1 = (remainder1 < 2) ? 0 : 11 - remainder1;
        if (digit1 != (cnpj.charAt(12) - '0')) {
            return false;
        }

        int sum2 = 0;
        for (int i = 0; i < 13; i++) {
            sum2 += (cnpj.charAt(i) - '0') * WEIGHTS_SECOND[i];
        }
        int remainder2 = sum2 % 11;
        int digit2 = (remainder2 < 2) ? 0 : 11 - remainder2;
        return digit2 == (cnpj.charAt(13) - '0');
    }
}
