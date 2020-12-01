package io.github.learnhydra.controller;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class UserInfoController {

	@GetMapping("/userinfo")
	public Mono<Map<String, Object>> userinfo(Authentication authentication) {
		Map<String, Object> response = Map.of("anonymous", true);
		if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
			Jwt token = (Jwt) authentication.getPrincipal();
			System.out.println("Resource 1 received token: " + token.getTokenValue());
			response = Map.of(

					"sub", token.getSubject(),

					"aud", token.getAudience(),

					"fullName", token.getClaim("fullName"),

					"groups", token.getClaimAsStringList("groups"),

					"expiresIn", (token.getExpiresAt().toEpochMilli() - System.currentTimeMillis()) / 1000

			);
		}
		return Mono.just(response);
	}
}
