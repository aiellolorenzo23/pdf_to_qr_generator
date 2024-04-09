package com.it.qr_download.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI opOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("QRGenerator from PDF - REST API")
                        .description("QRGenerator from PDF Spring Boot application")
                        .version("v0.0.1")
                        .license(new License().name("springdoc-openapi v2.2.0 Wiki").url("http://springdoc.org")))
                .externalDocs(new ExternalDocumentation()
                        .description("PornHub - Official Website")
                        .url("https://www.pornhub.com"));
    }
}