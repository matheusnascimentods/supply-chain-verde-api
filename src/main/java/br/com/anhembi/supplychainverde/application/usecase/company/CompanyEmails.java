package br.com.anhembi.supplychainverde.application.usecase.company;

import java.util.regex.Pattern;

final class CompanyEmails {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private CompanyEmails() {}

    static boolean isValid(String email) {
        return email != null && EMAIL.matcher(email).matches();
    }
}
