package com.emilhuseynov.accesspass.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    @Bean
    OpenAPI accessPassOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Access Pass Service")
                        .version("v1")
                        .description("Issues signed QR access passes and verifies them at gate devices."))
                .components(new Components()
                        .addSecuritySchemes("bearerJwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSecuritySchemes("deviceKey", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Device-Key")))
                .addSecurityItem(new SecurityRequirement().addList("bearerJwt"));
    }
}
