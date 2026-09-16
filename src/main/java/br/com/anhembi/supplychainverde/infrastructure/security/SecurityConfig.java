package br.com.anhembi.supplychainverde.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Token inválido ou ausente.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Acesso negado para este perfil.\"}");
                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/batches/*/traceability").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/batches/*/carbon-footprint").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/suppliers").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/suppliers/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/suppliers/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/suppliers/*/certifications").hasAnyRole("SUPPLIER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/certifications/**").hasAnyRole("AUDITOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/certifications/**").hasAnyRole("AUDITOR", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/products").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/batches").hasAnyRole("SUPPLIER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/suppliers/*/batches").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/batches/*/stages").hasAnyRole("SUPPLIER", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/batches/*/stages").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/stages/*/transport").hasAnyRole("SUPPLIER", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/stages/*/emission").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/suppliers/*/reports").hasAnyRole("AUDITOR", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/reports/**").hasAnyRole("AUDITOR", "MANAGER", "ADMIN", "SUPPLIER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/audit-logs/**").hasAnyRole("ADMIN", "AUDITOR")
                        .anyRequest().authenticated());

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
