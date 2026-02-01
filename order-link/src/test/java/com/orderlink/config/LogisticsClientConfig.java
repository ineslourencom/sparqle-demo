package com.orderlink.config;

import org.springframework.boot.test.context.TestConfiguration;

import lombok.Data;

@TestConfiguration
@Data
public class LogisticsClientConfig {

    private ApiConfig api = new ApiConfig();

    @Data
    public static class ApiConfig {
        private String baseUrl = "https://mock-carrier.onrender.com";
        private String apiKey = "carrier_api_key_1";
        private String webhookUrl = "https://localhost/logistics/webhook";
    }
}