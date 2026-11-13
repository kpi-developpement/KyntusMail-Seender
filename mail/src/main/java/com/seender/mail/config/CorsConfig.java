package com.seender.mail.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS restreint aux origines du front Kyntus.
 *
 * Un "*" combiné à allowCredentials laisse n'importe quel site appeler l'API
 * avec les cookies du navigateur de l'utilisateur : la liste est donc explicite
 * et surchargeable par la variable d'environnement KYNTUS_CORS_ALLOWED_ORIGINS.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${kyntus.cors.allowed-origins:http://10.10.10.25:1193,http://localhost:1193,http://localhost:3000}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept", "Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
