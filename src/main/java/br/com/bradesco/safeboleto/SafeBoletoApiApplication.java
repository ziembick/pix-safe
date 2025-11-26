package br.com.bradesco.safeboleto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class SafeBoletoApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SafeBoletoApiApplication.class, args);
	}

}
