package br.com.anhembi.supplychainverde.infrastructure.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI supplyChainVerdeOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Supply Chain Verde API")
                .version("v1")
                .description("API de rastreabilidade, emissões de CO2 e sustentabilidade."));
    }
}
