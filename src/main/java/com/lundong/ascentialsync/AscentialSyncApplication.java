package com.lundong.ascentialsync;

import com.lundong.ascentialsync.config.CustomServletAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AscentialSyncApplication {

	public static void main(String[] args) {
		SpringApplication.run(AscentialSyncApplication.class, args);
	}

	// 注入扩展实例到 IOC 容器
	@Bean
	public CustomServletAdapter getServletAdapter() {
		return new CustomServletAdapter();
	}

}
