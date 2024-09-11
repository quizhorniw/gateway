package com.drevotiuk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for application-specific beans.
 * <p>
 * This class contains Spring configuration settings and bean definitions for
 * the application context.
 * </p>
 */
@Configuration
public class AppConfig {

  /**
   * Creates and provides a {@link RestTemplate} bean.
   * 
   * @return a new instance of {@link RestTemplate}
   */
  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
