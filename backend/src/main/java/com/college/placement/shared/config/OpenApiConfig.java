package com.college.placement.shared.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT RS256 access token obtained from /auth/login or /auth/register"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI placementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Placement Intelligence & Skill Verification Platform API")
                        .version("1.0.0")
                        .description("REST API for managing student placements, companies, certificates, and job applications")
                        .contact(new Contact().name("Placement Office")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
