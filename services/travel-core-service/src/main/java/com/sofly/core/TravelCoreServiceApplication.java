package com.sofly.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing 
@EnableFeignClients
@ConfigurationPropertiesScan 
public class TravelCoreServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TravelCoreServiceApplication.class, args);
	}

}
