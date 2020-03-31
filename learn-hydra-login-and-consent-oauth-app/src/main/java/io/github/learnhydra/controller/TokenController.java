package io.github.learnhydra.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;

/**
 * Spring doesn't currently support JWT and Opaque tokens at the same time:
 *
 * https://github.com/spring-projects/spring-boot/issues/19426
 */
@Controller
@RequestMapping("/")
public class TokenController {

	@Value("${application.service-url}")
	protected String serviceUrl;

	@Autowired
	private WebClient.Builder webClientBuilder;

	@Autowired
	private ReactiveOAuth2AuthorizedClientService oAuth2AuthorizedClientService;
	
	@Autowired
	private ServerOAuth2AuthorizedClientExchangeFilterFunction oauthFilter;

	@GetMapping("/propagate-credentials")
	public Mono<String> propagateCredentials(Authentication authentication) {
		return withOAuthToken(authentication)
			.flatMap(token -> {
				return webClientBuilder
					.baseUrl(serviceUrl)
					.build()
					.get()
					.uri(uri -> uri.path("/api/userinfo").build())
					.attributes(getClientRegistrationId(authentication))
					.headers(headers -> {
						headers.setBearerAuth(token);
					})
					.accept(MediaType.APPLICATION_JSON)
					.exchange()
					.flatMap(response -> {
						if (response.statusCode().equals(HttpStatus.OK)) {
							return response.bodyToMono(JsonNode.class).flatMap(json -> {
								String sub = json.path("sub").asText("");
								String fullName = json.path("fullName").asText("");
								try {
									sub = URLEncoder.encode(sub, "UTF-8");
									fullName = URLEncoder.encode(fullName, "UTF-8");
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								}
								return Mono.just(String.format("redirect:/?success=&sub=%s&fullName=%s", sub, fullName));
							});
						}
						else {
							return Mono.just("redirect:/?" + response.statusCode().name());
						}
					})
					.doOnError(e -> e.printStackTrace())
					.onErrorReturn("redirect:/?error");
			})
			.switchIfEmpty(Mono.just("redirect:/?no_token"));
	}
	
	@GetMapping("/client-credentials")
	public Mono<String> clientCredentials() {
		return webClientBuilder
			.baseUrl(serviceUrl)
			.filter(oauthFilter)
			.build()
			.get()
			.uri(uri -> uri.path("/api/userinfo").build())
			.attributes(
					ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("hydra-service"))
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.flatMap(response -> {
				if (response.statusCode().equals(HttpStatus.OK)) {
						return response.bodyToMono(JsonNode.class).flatMap(json -> {
							String sub = json.path("sub").asText("");
							try {
								sub = URLEncoder.encode(sub, "UTF-8");
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
							return Mono.just("redirect:/?success=&sub=" + sub);
						});
				}
				else {
					return Mono.just("redirect:/?" + response.statusCode().name());
				}
			})
			.doOnError(e -> e.printStackTrace())
			.onErrorReturn("redirect:/?error");
	}
	
	private Mono<String> withOAuthToken(Authentication authentication) {
		return Mono.justOrEmpty(authentication)
				.filter(a -> a instanceof OAuth2AuthenticationToken)
				.cast(OAuth2AuthenticationToken.class)
				.flatMap(token -> Mono.just(token.getPrincipal()))
				.filter(p -> p instanceof DefaultOidcUser)
				.cast(DefaultOidcUser.class)
				.flatMap(user -> Mono.just(user.getIdToken().getTokenValue()))
				.switchIfEmpty(getAccessToken(authentication));
	}

	private Mono<String> getAccessToken(Authentication authentication) {
		return Mono.justOrEmpty(authentication).filter(a -> a instanceof OAuth2AuthenticationToken)
				.cast(OAuth2AuthenticationToken.class).flatMap(token -> {
					return oAuth2AuthorizedClientService
							.loadAuthorizedClient(token.getAuthorizedClientRegistrationId(), token.getName())
							.flatMap(client -> {
								String accessToken = client.getAccessToken().getTokenValue();
								return Mono.just(accessToken);
							});
				});
	}
	
	private Consumer<Map<String, Object>> getClientRegistrationId(Authentication authentication) {
		String clientRegistrationId = "hydra-service";
		if (authentication instanceof OAuth2AuthenticationToken) {
			OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
			clientRegistrationId = token.getAuthorizedClientRegistrationId();
		}
		return ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(clientRegistrationId);
	}
}
