package br.com.anhembi.supplychainverde.infrastructure.web.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@io.swagger.v3.oas.annotations.security.SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Informe somente o token JWT. O prefixo Bearer é adicionado automaticamente."
)
public class SwaggerConfig {
    @Bean
    public OpenAPI supplyChainVerdeOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Supply Chain Verde API")
                .version("v1")
                .description("API de rastreabilidade, emissões de CO2 e sustentabilidade. "
                        + "Use o botão Authorize para informar o token JWT."))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
