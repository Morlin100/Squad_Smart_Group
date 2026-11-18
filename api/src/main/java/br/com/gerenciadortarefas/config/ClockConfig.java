package br.com.gerenciadortarefas.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Relógio no fuso de Brasília, usado para "hoje" e "últimos 7 dias". Nos testes dá para fixar a data. */
@Configuration
public class ClockConfig {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Bean
    Clock clock() {
        return Clock.system(FUSO);
    }
}
