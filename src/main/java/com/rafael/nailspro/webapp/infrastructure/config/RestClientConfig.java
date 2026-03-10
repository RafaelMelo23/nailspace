package com.rafael.nailspro.webapp.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClientConfig(RestClient.Builder builder) {
        return builder.build();
    }
}
