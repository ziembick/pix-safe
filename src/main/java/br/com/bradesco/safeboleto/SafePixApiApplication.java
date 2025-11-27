package br.com.bradesco.safeboleto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class SafePixApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SafePixApiApplication.class, args);
	}

}

