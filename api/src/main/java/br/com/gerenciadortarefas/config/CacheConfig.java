package br.com.gerenciadortarefas.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Cache em memória (HU07 - KAN-86). Funciona porque a API roda com uma única instância no Azure.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String DASHBOARD = "dashboard";
}
