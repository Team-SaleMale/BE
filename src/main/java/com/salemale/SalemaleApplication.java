package com.salemale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // 이거 추가
public class SalemaleApplication {

	public static void main(String[] args) {
		SpringApplication.run(SalemaleApplication.class, args);
	}

}
