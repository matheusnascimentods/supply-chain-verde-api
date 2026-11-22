package br.com.anhembi.supplychainverde.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/**
 * Rate limit da feature 013. Roda depois do JwtAuthenticationFilter para contar por usuário (RN-02), por isso entra
 * na cadeia do SecurityConfig e não é @Component: como filtro do servlet, rodaria antes da autenticação.
 */
// ponytail: janela fixa em memória. Aceita até 2x o limite na virada do minuto e vale para uma instância só.
// Com mais de uma réplica, trocar por Bucket4j + Redis.
public class RateLimitFilter extends OncePerRequestFilter {
    static final long WINDOW_MS = 60_000;
    static final int GLOBAL_LIMIT = 120;
    // RN-04. Prefixo de "método URI": "GET /api/v1/invites/" cobre o {token}.
    static final Map<String, Integer> STRICT_LIMITS = Map.of(
            "POST /api/v1/auth/login", 5,
            "POST /api/v1/invites/accept", 5,
            "GET /api/v1/invites/", 10,
            "POST /api/v1/companies", 3);

    private final Map<String, AtomicInteger> hits = new ConcurrentHashMap<>();
    private final LongSupplier now;
    private volatile long windowStart;

    public RateLimitFilter(LongSupplier now) {
        this.now = now;
        this.windowStart = now.getAsLong();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long current = now.getAsLong();
        // RN-05: zerar o mapa inteiro na virada mantém o tamanho dele nos clientes de um minuto, sem rotina de limpeza.
        if (current - windowStart >= WINDOW_MS) {
            synchronized (this) {
                if (current - windowStart >= WINDOW_MS) {
                    hits.clear();
                    windowStart = current;
                }
            }
        }

        String ip = "ip:" + request.getRemoteAddr();
        String route = request.getMethod() + " " + request.getRequestURI();
        String strict = STRICT_LIMITS.keySet().stream().filter(route::startsWith).findFirst().orElse(null);

        if (exceeded(clientKey(ip), GLOBAL_LIMIT)
                || strict != null && exceeded(ip + " " + strict, STRICT_LIMITS.get(strict))) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf((windowStart + WINDOW_MS - current) / 1000 + 1));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"message\":\"Muitas requisições. Tente novamente em instantes.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean exceeded(String key, int limit) {
        return hits.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet() > limit;
    }

    // RN-02: com JWT válido conta o usuário; sem ele, ou com JWT inválido, conta o IP.
    private static String clientKey(String ip) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof CustomUserPrincipal principal
                ? "u:" + principal.userId()
                : ip;
    }
}
