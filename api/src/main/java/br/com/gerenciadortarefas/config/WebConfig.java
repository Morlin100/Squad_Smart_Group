package br.com.gerenciadortarefas.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Telas estáticas servidas pela API: /pesquisa (pesquisa de satisfação) e /poker. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/pesquisa").setViewName("forward:/pesquisa/index.html");
        registry.addViewController("/pesquisa/").setViewName("forward:/pesquisa/index.html");
        registry.addViewController("/poker").setViewName("forward:/poker/index.html");
        registry.addViewController("/poker/").setViewName("forward:/poker/index.html");
    }
}
