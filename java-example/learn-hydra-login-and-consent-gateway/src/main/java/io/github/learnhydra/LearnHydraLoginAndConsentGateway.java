package io.github.learnhydra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@EnableEurekaClient
@SpringBootApplication
public class LearnHydraLoginAndConsentGateway {

	public static void main(String[] args) {
		SpringApplication.run(LearnHydraLoginAndConsentGateway.class, args);
	}

}
