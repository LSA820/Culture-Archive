package com.example.culture_archive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling; // import 추가


@EnableScheduling
@EnableJpaAuditing
@EnableAsync
@SpringBootApplication
public class CultureArchiveApplication {
	public static void main(String[] args) {
		SpringApplication.run(CultureArchiveApplication.class, args);
	}
}