package com.smarsh.dataengineering.vams.process;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.smarsh.dataengineering.vams.process.eml.EmlEncryptionService;

@SpringBootApplication
public class VamsByokEncrypterApplication {

	public static void main(String[] args) {
		SpringApplication.run(VamsByokEncrypterApplication.class, args);
	}

	@Bean
	CommandLineRunner run(EmlEncryptionService service) {
		return args -> service.processAllEmls();
	}
}
