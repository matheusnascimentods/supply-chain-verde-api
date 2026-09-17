package br.com.anhembi.supplychainverde.infrastructure.audit;

import br.com.anhembi.supplychainverde.domain.entity.AuditLog;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.AuditAction;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.repository.AuditLogRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl.SupplierRepositoryImpl;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditLogInterceptorTest {

    private AuditLogRepository auditLogRepository;
    private UserRepository userRepository;
    private AuditLogInterceptor interceptor;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        userRepository = mock(UserRepository.class);
        interceptor = new AuditLogInterceptor(auditLogRepository, userRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateAuditLogWhenSavingAnEntity() {
        User currentUser = User.builder()
                .userId(10L)
                .name("Alice")
                .email("alice@example.com")
                .role(UserRole.ADMIN)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(currentUser));

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                new CustomUserPrincipal(10L, "alice@example.com", "ADMIN"),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Supplier supplier = new Supplier();
        supplier.setName("Fornecedor Verde");

        JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{supplier});
        when(joinPoint.getTarget()).thenReturn(mock(SupplierRepositoryImpl.class));

        interceptor.logSaveAudit(joinPoint, supplier);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog auditLog = captor.getValue();
        assertThat(auditLog.getUser().getUserId()).isEqualTo(10L);
        assertThat(auditLog.getAction()).isEqualTo(AuditAction.INSERT);
        assertThat(auditLog.getAffectedTable()).isEqualTo("supplier");
        assertThat(auditLog.getPerformedAt()).isNotNull();
    }

}
