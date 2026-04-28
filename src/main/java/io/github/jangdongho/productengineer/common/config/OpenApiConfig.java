package io.github.jangdongho.productengineer.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "Product Engineer Class API",
            version = "v1",
            description = "강의(클래스) · 수강 신청 API 문서",
            license = @License(name = "Apache 2.0")))
public class OpenApiConfig {

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI().servers(List.of(new Server().url("/").description("Default server")));
  }
}
