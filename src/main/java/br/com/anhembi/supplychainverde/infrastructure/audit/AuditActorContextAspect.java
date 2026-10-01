package br.com.anhembi.supplychainverde.infrastructure.audit;

import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditActorContextAspect {
    private static final String SET_ACTOR_SQL = "SELECT set_config('app.user_id', ?, true)";

    private final JdbcTemplate jdbcTemplate;

    @Before("execution(* br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl.*RepositoryImpl.save(..))"
            + " || execution(* br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl.*RepositoryImpl.deleteById(..))")
    public void setAuthenticatedActorForTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Audited writes require an active transaction.");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        String actorId = principal instanceof CustomUserPrincipal currentUser
                ? currentUser.userId().toString()
                : "";

        jdbcTemplate.queryForObject(SET_ACTOR_SQL, String.class, actorId);
    }
}
