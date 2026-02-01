package com.orderlink;

import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync(proxyTargetClass = true)
public class TestOrderLinkApplication {

	public static void main(String[] args) {
		SpringApplication.from(OrderLinkApplication::main).run(args);
	}

}
