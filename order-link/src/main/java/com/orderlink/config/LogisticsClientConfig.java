package com.orderlink.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;



@Configuration
@Data
@ConfigurationProperties(prefix = "logistics")
public class LogisticsClientConfig {

    private ApiConfig api;

    @Data
    @Configuration
    public static class ApiConfig {
        private String baseUrl;
        private String apiKey;
        private String webhookUrl;
    }
}
