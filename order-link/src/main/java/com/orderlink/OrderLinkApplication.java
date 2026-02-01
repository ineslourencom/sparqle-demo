package com.orderlink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableFeignClients
@EnableAsync(proxyTargetClass = true)
@SpringBootApplication
public class OrderLinkApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderLinkApplication.class, args);
	}

}
