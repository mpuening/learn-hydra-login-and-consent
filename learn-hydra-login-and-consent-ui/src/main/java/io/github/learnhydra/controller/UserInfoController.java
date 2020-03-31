package io.github.learnhydra.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;

@RestController
public class UserInfoController {

	@Value("${hydra.oauth-private-url}")
	protected String hydraUrl;

	@Autowired
	private WebClient.Builder webClientBuilder;

	@GetMapping("/api/userinfo")
	@PreAuthorize("hasAuthority('SCOPE_profile.read')")
	public Mono<JsonNode> userinfo(Authentication authentication, ServerWebExchange exchange) {
		String token = exchange.getRequest().getHeaders().getFirst("Authorization");
		return webClientBuilder
				.baseUrl(hydraUrl)
				.build()
				.get()
				.uri(uri -> uri.path("/userinfo").build())
				.headers(headers -> {
					headers.set("Authorization", token);
				})
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.flatMap(response -> {
					return response.bodyToMono(JsonNode.class);
				})
				.doOnError(e -> e.printStackTrace());
	}
}
