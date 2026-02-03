package com.orderlink.config;

import static java.util.concurrent.TimeUnit.SECONDS;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.orderlink.logistic.client.LateLogisticsClient;

import feign.Logger;
import feign.Retryer;
import feign.slf4j.Slf4jLogger;


@Configuration
public class LateLogisticsFeignConfig {

    @Bean
    public Retryer lateLogisticsRetryer() {
        long period = 200L;
        long maxPeriod = SECONDS.toMillis(10);
        int maxAttempts = 10;
        return new Retryer.Default(period, maxPeriod, maxAttempts);
    }

    @Bean
    public Logger lateLogisticsLogger() {
        return new Slf4jLogger(LateLogisticsClient.class);
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL; 
    }
}