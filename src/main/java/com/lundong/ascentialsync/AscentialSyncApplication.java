package com.lundong.ascentialsync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AscentialSyncApplication {

	public static void main(String[] args) {
		SpringApplication.run(AscentialSyncApplication.class, args);
	}

}
