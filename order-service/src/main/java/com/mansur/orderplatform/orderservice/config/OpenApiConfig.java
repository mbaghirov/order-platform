package com.mansur.orderplatform.orderservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Order Service")
                .version("1.0.0")
                .description("Products, stock and orders. Every order change is published to Kafka "
                        + "and picked up by notification-service."));
    }
}
