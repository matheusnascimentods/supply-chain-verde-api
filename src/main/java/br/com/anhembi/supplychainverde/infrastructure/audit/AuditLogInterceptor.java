package br.com.anhembi.supplychainverde.infrastructure.audit;

import br.com.anhembi.supplychainverde.domain.entity.AuditLog;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.AuditAction;
import br.com.anhembi.supplychainverde.domain.repository.AuditLogRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogInterceptor {
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @AfterReturning(
            pointcut = "execution(* br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl.*RepositoryImpl.save(..)) && !execution(* br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl.AuditLogRepositoryImpl.save(..))",
            returning = "result"
    )
    public void logSaveAudit(JoinPoint joinPoint, Object result) {
        if (result == null || joinPoint.getArgs().length == 0) {
            return;
        }

        User user = resolveCurrentUser();
        if (user == null) {
            return;
        }

        Object persistedEntity = joinPoint.getArgs()[0];
        if (persistedEntity == null) {
            return;
        }

        AuditLog auditLog = AuditLog.builder()
                .user(user)
                .action(resolveSaveAction(persistedEntity))
                .affectedTable(resolveTableName(joinPoint.getTarget().getClass()))
                .performedAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
    }

    @AfterReturning(
            pointcut = "execution(* br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl.*RepositoryImpl.deleteById(..)) && !execution(* br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl.AuditLogRepositoryImpl.deleteById(..))"
    )
    public void logDeleteAudit(JoinPoint joinPoint) {
        User user = resolveCurrentUser();
        if (user == null) {
            return;
        }

        AuditLog auditLog = AuditLog.builder()
                .user(user)
                .action(AuditAction.DELETE)
                .affectedTable(resolveTableName(joinPoint.getTarget().getClass()))
                .performedAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
    }

    private User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null || !(authentication.getPrincipal() instanceof CustomUserPrincipal principal)) {
            return null;
        }

        return userRepository.findById(principal.userId()).orElse(null);
    }

    private AuditAction resolveSaveAction(Object entity) {
        Object idValue = resolveEntityId(entity);
        return idValue == null ? AuditAction.INSERT : AuditAction.UPDATE;
    }

    private Object resolveEntityId(Object entity) {
        for (Method method : entity.getClass().getMethods()) {
            if (method.getParameterCount() == 0
                    && method.getName().endsWith("Id")
                    && (method.getReturnType() == Long.class || method.getReturnType() == long.class || Number.class.isAssignableFrom(method.getReturnType()))) {
                try {
                    return method.invoke(entity);
                } catch (Exception ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private String resolveTableName(Class<?> repositoryClass) {
        String simpleName = repositoryClass.getSimpleName();
        String rawName = simpleName.replace("RepositoryImpl", "");

        if (rawName.equals("User")) {
            return "users";
        }

        return camelToSnake(rawName);
    }

    private String camelToSnake(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                builder.append('_');
            }
            builder.append(Character.toLowerCase(c));
        }
        return builder.toString();
    }
}
