package com.garbed.document_management_service.configuration;

import com.garbed.document_management_service.util.SystemConstants;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenApiConfig class used for OpenApiConfig in the system.
 *
 * @author Erick Garcia
 * @version 1.0.0
 * @since 6/30/25
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title(SystemConstants.Config.PROJECT_TITLE)
                .version(SystemConstants.Config.API_VERSION)
                .description(SystemConstants.Config.PROJECT_DESCRIPTION));
  }
}
