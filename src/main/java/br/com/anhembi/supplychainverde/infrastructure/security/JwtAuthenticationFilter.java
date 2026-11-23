package br.com.anhembi.supplychainverde.infrastructure.security;

import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            if (jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                String email = jwtTokenProvider.getEmailFromToken(token);
                String role = jwtTokenProvider.getRoleFromToken(token);
                Long companyId = jwtTokenProvider.getCompanyIdFromToken(token);
                CompanyType companyType = jwtTokenProvider.getCompanyTypeFromToken(token);
                // Sem companhia, as listagens não filtram nada: só vale para o PLATFORM_ADMIN. Tokens antigos, sem
                // companyId, ficam sem autenticação em vez de enxergar tudo.
                boolean scoped = "PLATFORM_ADMIN".equals(role) ? companyId == null : companyId != null && companyType != null;

                // RN-11 (016): o token de quem foi desativado deixa de valer na próxima requisição.
                // ponytail: 1 SELECT por PK a cada request; cache curto (~30s) se aparecer no profiling.
                if (userId != null && email != null && role != null && scoped && userRepository.isActive(userId)) {
                    CustomUserPrincipal principal = new CustomUserPrincipal(userId, email, role, companyId, companyType);
                    List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                    Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
