package br.com.anhembi.supplychainverde.infrastructure.audit;

import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditActorContextAspect {
    private static final String SET_ACTOR_SQL = "SET @app_user_id = NULLIF(?, '')";
    private static final String CLEAR_ACTOR_SQL = "SET @app_user_id = NULL";

    private final JdbcTemplate jdbcTemplate;

    @Before("execution(* br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl.*RepositoryImpl.save(..))"
            + " || execution(* br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl.*RepositoryImpl.deleteById(..))"
            + " || execution(* br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl.*RepositoryImpl.generate(..))")
    public void setAuthenticatedActorForTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Audited writes require an active transaction.");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        String actorId = principal instanceof CustomUserPrincipal currentUser
                ? currentUser.userId().toString()
                : "";

        jdbcTemplate.update(SET_ACTOR_SQL, actorId);
        // A variável vale para a sessão, e o pool reaproveita a conexão: limpa antes de devolvê-la.
        // No afterCompletion a conexão da transação ainda está vinculada à thread.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                jdbcTemplate.update(CLEAR_ACTOR_SQL);
            }
        });
    }
}
