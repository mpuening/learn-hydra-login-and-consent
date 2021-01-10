package io.github.learnhydra.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;

import io.github.learnhydra.config.OpenIdTokenExchangeFilterFunction;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/")
public class TokenController {

	@Value("${application.gateway-url}")
	private String gatewayUrl;

	@Autowired
	private WebClient.Builder webClientBuilder;

	@Autowired
	private ReactiveOAuth2AuthorizedClientService oAuth2AuthorizedClientService;

	@Autowired
	private ReactiveClientRegistrationRepository clientRegistrationRepository;

	@Autowired
	private ServerOAuth2AuthorizedClientRepository authorizedClientRepository;
	
	private ExchangeFilterFunction openIdExchangeFilter;

	private ExchangeFilterFunction authCodeExchangeFilter;

	private ExchangeFilterFunction clientCredentialsExchangeFilter;

	@PostConstruct
	public void setupExchangeFilters() {
		openIdExchangeFilter = new OpenIdTokenExchangeFilterFunction();
		authCodeExchangeFilter = serverOAuth2AuthorizedClientExchangeFilterFunction("hydra-code", true);
		clientCredentialsExchangeFilter = serverOAuth2AuthorizedClientExchangeFilterFunction("hydra-service", false);
	}
	
	@GetMapping("/openid-token")
	public Mono<String> opentoken(Authentication authentication) {
		return withOpenIdToken(authentication)
			.flatMap(token -> {
				System.out.println("Client received token: " + token);
				try {
					token = URLEncoder.encode(token, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				return Mono.just(String.format("redirect:/?success=&token=%s", token));
			})
			.switchIfEmpty(Mono.just("redirect:/?no_token="));
	}

	@GetMapping("/access-token")
	public Mono<String> accessToken(Authentication authentication) {
		return getAccessToken(authentication)
			.flatMap(token -> {
				System.out.println("Client received token: " + token);
				try {
					token = URLEncoder.encode(token, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				return Mono.just(String.format("redirect:/?success=&token=%s", token));
			})
			.switchIfEmpty(Mono.just("redirect:/?no_token="));
	}

	@GetMapping("/refresh-token")
	public Mono<String> refreshToken(Authentication authentication) {
		return getRefreshToken(authentication)
			.flatMap(token -> {
				System.out.println("Client received token: " + token);
				try {
					token = URLEncoder.encode(token, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				return Mono.just(String.format("redirect:/?success=&token=%s", token));
			})
			.switchIfEmpty(Mono.just("redirect:/?no_token="));
	}

	/**
	 * Resource 1 accepts JWT's and thus we send it our OpenID token (which is a bad
	 * practice by the way). This will fail if we log via access token only.
	 */
	@GetMapping("/propagate-credentials-to-resource1")
	public Mono<String> propagateCredentialsToResource1(Authentication authentication) {
		ExchangeFilterFunction oauth = openIdExchangeFilter;
		return webClientBuilder
			.clone()
			.baseUrl(gatewayUrl)
			.filter(oauth)
			.build()
			.get()
			.uri(uri -> uri.path("/resource1/api/userinfo").build())
			.attributes(ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("hydra-openid"))
			.accept(MediaType.APPLICATION_JSON)
			.exchangeToMono(response -> {
				if (response.statusCode().equals(HttpStatus.OK)) {
					return response.bodyToMono(JsonNode.class).single().flatMap(json -> {
						String sub = json.path("sub").asText("");
						String fullName = json.path("fullName").asText("");
						long expiresIn = json.path("expiresIn").asLong(-1L);
						try {
							sub = URLEncoder.encode(sub, "UTF-8");
							fullName = URLEncoder.encode(fullName, "UTF-8");
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
						return Mono.just(String.format("redirect:/?success=&sub=%s&fullName=%s&expiresIn=%d", sub, fullName, expiresIn));
					});
				}
				else {
					return Mono.just("redirect:/?error=" + response.statusCode().name());
				}
			})
			.doOnError(e -> e.printStackTrace())
			.onErrorReturn("redirect:/?error=");
	}

	@GetMapping("/propagate-credentials-to-resource2")
	public Mono<String> propagateCredentialsToResource2(Authentication authentication) {
		return propagateCredentials(authentication, "/resource2/api/userinfo");
	}

	@GetMapping("/propagate-credentials-to-resource3")
	public Mono<String> propagateCredentialsToResource3(Authentication authentication) {
		return propagateCredentials(authentication, "/resource3/api/userinfo");
	}

	/**
	 * Resource 2 & 3 accepts access tokens. The access token can be held under a different
	 * registration depending on how we logged in.
	 */
	private Mono<String> propagateCredentials(Authentication authentication, String path) {
		boolean isOpenId = authentication != null
				&& authentication.getAuthorities().contains(new SimpleGrantedAuthority("SCOPE_openid"));
		String clientRegistrationId = isOpenId ? "hydra-openid" : "hydra-code";
		ExchangeFilterFunction oauth = authCodeExchangeFilter;
		return webClientBuilder
			.clone()
			.baseUrl(gatewayUrl)
			.filter(oauth)
			.build()
			.get()
			.uri(uri -> uri.path(path).build())
			.attributes(ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(clientRegistrationId))
			.accept(MediaType.APPLICATION_JSON)
			.exchangeToMono(response -> {
				if (response.statusCode().equals(HttpStatus.OK)) {
					return response.bodyToMono(JsonNode.class).single().flatMap(json -> {
						String sub = json.path("sub").asText("");
						String clientId = json.path("clientId").asText("");
						long expiresIn = json.path("expiresIn").asLong(-1L);
						try {
							sub = URLEncoder.encode(sub, "UTF-8");
							clientId = URLEncoder.encode(clientId, "UTF-8");
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
						return Mono.just(String.format("redirect:/?success=&sub=%s&clientId=%s&expiresIn=%d", sub, clientId, expiresIn));
					});
				}
				else {
					return Mono.just("redirect:/?error=" + response.statusCode().name());
				}
			})
			.doOnError(e -> e.printStackTrace())
			.onErrorReturn("redirect:/?error=");
	}

	/**
	 * There isn't a way for this to succeed.
	 */
	@GetMapping("/client-credentials-to-resource1")
	public Mono<String> clientCredentialsToResource1() {
		// We expect this to fail since resource1 expects a JWT
		return clientCredentials("/resource1/api/userinfo");
	}

	@GetMapping("/client-credentials-to-resource2")
	public Mono<String> clientCredentialsToResource2() {
		return clientCredentials("/resource2/api/userinfo");
	}

	@GetMapping("/client-credentials-to-resource3")
	public Mono<String> clientCredentialsToResource3() {
		return clientCredentials("/resource3/api/userinfo");
	}

	/**
	 * Resource 2 & 3 accepts access tokens. In this case we are using client
	 * credentials to obtain a token.
	 */
	private Mono<String> clientCredentials(String path) {
		ExchangeFilterFunction oauth = clientCredentialsExchangeFilter;
		return webClientBuilder
			.clone()
			.baseUrl(gatewayUrl)
			.filter(oauth)
			.build()
			.get()
			.uri(uri -> uri.path(path).build())
			.attributes(ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("hydra-service"))
			.accept(MediaType.APPLICATION_JSON)
			.exchangeToMono(response -> {
				if (response.statusCode().equals(HttpStatus.OK)) {
						return response.bodyToMono(JsonNode.class).single().flatMap(json -> {
							String sub = json.path("sub").asText("");
							String clientId = json.path("clientId").asText("");
							long expiresIn = json.path("expiresIn").asLong(-1L);
							try {
								sub = URLEncoder.encode(sub, "UTF-8");
								clientId = URLEncoder.encode(clientId, "UTF-8");
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
							return Mono.just(String.format("redirect:/?success=&sub=%s&clientId=%s&expiresIn=%d", sub, clientId, expiresIn));
						});
				}
				else {
					return Mono.just("redirect:/?error=" + response.statusCode().name());
				}
			})
			.doOnError(e -> e.printStackTrace())
			.onErrorReturn("redirect:/?error=");
	}

	/**
	 * Get token for OpenId Login
	 */
	private Mono<String> withOpenIdToken(Authentication authentication) {
		return Mono.justOrEmpty(authentication)
				.filter(a -> a instanceof OAuth2AuthenticationToken)
				.cast(OAuth2AuthenticationToken.class)
				.flatMap(token -> Mono.just(token.getPrincipal()))
				.filter(p -> p instanceof OidcUser)
				.cast(OidcUser.class)
				.flatMap(user -> Mono.just(user.getIdToken().getTokenValue()));
	}

	/**
	 * Get access token
	 */
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

	/**
	 * Get refresh token
	 */
	private Mono<String> getRefreshToken(Authentication authentication) {
		return Mono.justOrEmpty(authentication).filter(a -> a instanceof OAuth2AuthenticationToken)
				.cast(OAuth2AuthenticationToken.class).flatMap(token -> {
					return oAuth2AuthorizedClientService
							.loadAuthorizedClient(token.getAuthorizedClientRegistrationId(), token.getName())
							.flatMap(client -> {
								String refreshToken = client.getRefreshToken().getTokenValue();
								return Mono.just(refreshToken);
							});
				});
	}

	/**
	 * Create OAuth Filter to manage tokens
	 */
	private ServerOAuth2AuthorizedClientExchangeFilterFunction serverOAuth2AuthorizedClientExchangeFilterFunction(
			String clientRegistrationId, boolean isDefaultOAuth2AuthorizedClient) {
		ServerOAuth2AuthorizedClientExchangeFilterFunction oauth = new ServerOAuth2AuthorizedClientExchangeFilterFunction(
				clientRegistrationRepository, authorizedClientRepository);
		oauth.setDefaultClientRegistrationId(clientRegistrationId);
		oauth.setDefaultOAuth2AuthorizedClient(isDefaultOAuth2AuthorizedClient);
		return oauth;
	}

}
