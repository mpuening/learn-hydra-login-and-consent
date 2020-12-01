package io.github.learnhydra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@EnableEurekaClient
@SpringBootApplication
public class LearnHydraLoginAndConsentResource1 {

	public static void main(String[] args) {
		SpringApplication.run(LearnHydraLoginAndConsentResource1.class, args);
	}

}
