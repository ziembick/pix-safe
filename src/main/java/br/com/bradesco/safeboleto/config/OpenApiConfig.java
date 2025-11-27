package br.com.bradesco.safeboleto.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "Bearer Authentication";

        return new OpenAPI()
                .info(new Info()
                        .title("SafePix API")
                        .version("2.0")
                        .description("API para validação de transações PIX com detecção avançada de fraudes.\n\n" +
                                   "**Funcionalidades:**\n" +
                                   "- Validação de transações PIX com detecção de fraudes\n" +
                                   "- Sistema de score de risco (0-100)\n" +
                                   "- Autenticação JWT\n" +
                                   "- Histórico de validações\n\n" +
                                   "**Como usar:**\n" +
                                   "1. Faça login em `/api/auth/login` com username: `admin` e password: `password`\n" +
                                   "2. Copie o token JWT retornado\n" +
                                   "3. Clique em 'Authorize' no topo da página e cole o token\n" +
                                   "4. Agora você pode testar os endpoints protegidos")
                )
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(
                        new Components()
                                .addSecuritySchemes(securitySchemeName,
                                        new SecurityScheme()
                                                .name(securitySchemeName)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .in(SecurityScheme.In.HEADER)
                                                .description("Insira o token JWT aqui para autorizar o acesso.")
                                )
                );
    }
}