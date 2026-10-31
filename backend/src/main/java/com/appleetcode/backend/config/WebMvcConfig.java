package com.appleetcode.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Prefixa todo endpoint REST (@RestController) com /api. Sem isso, /stats
 * (endpoint da seção 6) colide com a rota Angular /stats (seção 7) -- os
 * dois precisam responder no mesmo domínio:porta em produção, então não
 * podem compartilhar o mesmo path.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api", HandlerTypePredicate.forAnnotation(RestController.class));
    }
}
