package com.ycyw.poc_chat.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@Configuration
@OpenAPIDefinition(
  info = @Info(
    title = "You Car Your Way",
    version = "v1",
    description = """
        🚙🚗**API de location de voiture**\n
        Ce PoC est centré sur la fonction Chat 💬\n
        *(WebSocket -STOMP - RabbitMQ)*
           
        Pour plus d’infos, consultez la doc technique
        """
  ),
  security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
  name = "bearerAuth",
  type = SecuritySchemeType.HTTP,
  scheme = "bearer",
  bearerFormat = "JWT"
)
public class SwaggerConfig {}
