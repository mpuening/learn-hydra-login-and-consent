package io.github.learnhydra.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
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
		if (authentication instanceof BearerTokenAuthentication) {
			System.out.println("Resource 2 received token: "
					+ ((BearerTokenAuthentication) authentication).getToken().getTokenValue());
		}
		if (authentication != null && authentication.getPrincipal() instanceof OAuth2AuthenticatedPrincipal) {
			OAuth2AuthenticatedPrincipal attributes = (OAuth2AuthenticatedPrincipal) authentication.getPrincipal();
			response = Map.of(

					"clientId", attributes.getAttribute("client_id"),

					"sub", attributes.getAttribute("sub"),

					"active", attributes.getAttribute("active"),

					"expiresIn", (((Instant)attributes.getAttribute("exp")).toEpochMilli() - System.currentTimeMillis()) / 1000

			);
		}
		return Mono.just(response);
	}
}
