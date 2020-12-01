package io.github.learnhydra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class LearnHydraLoginAndConsentDiscoveryServer {

	public static void main(String[] args) {
		SpringApplication.run(LearnHydraLoginAndConsentDiscoveryServer.class, args);
	}

}
