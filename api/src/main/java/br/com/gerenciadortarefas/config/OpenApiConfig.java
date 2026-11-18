package br.com.gerenciadortarefas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Gerenciador de Tarefas API")
                .description("API do módulo de cadastro de tarefas")
                .version("v1"));
    }
}
