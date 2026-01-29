package com.smarsh.dataengineering.byok.encrypter;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.smarsh.dataengineering.byok.encrypter.eml.EmlEncryptionService;

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
