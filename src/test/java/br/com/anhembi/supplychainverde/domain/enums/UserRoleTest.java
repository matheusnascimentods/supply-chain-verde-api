package br.com.anhembi.supplychainverde.domain.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserRoleTest {
    @Test
    void allowsOnlyTheRolesOfEachCompanyType() {
        assertThat(rolesFor(CompanyType.MARKET)).containsExactlyInAnyOrder(UserRole.ADMIN, UserRole.MANAGER, UserRole.AUDITOR);
        assertThat(rolesFor(CompanyType.SUPPLIER)).containsExactlyInAnyOrder(UserRole.ADMIN, UserRole.SUPPLIER);
        assertThat(rolesFor(null)).isEmpty();
    }

    private static java.util.List<UserRole> rolesFor(CompanyType type) {
        return java.util.Arrays.stream(UserRole.values()).filter(role -> role.allowedFor(type)).toList();
    }
}
