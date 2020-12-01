package io.github.learnhydra.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import reactor.core.publisher.Mono;

/**
 * This class is similar to ServerBearerExchangeFilterFunction except that it
 * supports OpenId Users.
 *
 * Note: This is VERY BAD practice to propagate the id token. This is for demonstration
 * purposes only.
 */
public class OpenIdTokenExchangeFilterFunction implements ExchangeFilterFunction {

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		return oauth2Token()
				.map(token -> bearer(request, token))
				.defaultIfEmpty(request)
				.flatMap(next::exchange);
	}

	private Mono<DefaultOidcUser> oauth2Token() {
		return currentAuthentication()
				.filter(authentication -> authentication.getPrincipal() instanceof DefaultOidcUser)
				.map(Authentication::getPrincipal)
				.cast(DefaultOidcUser.class);
	}
	
	private Mono<Authentication> currentAuthentication() {
		return ReactiveSecurityContextHolder.getContext()
				.map(SecurityContext::getAuthentication);
	}

	private ClientRequest bearer(ClientRequest request, DefaultOidcUser user) {
		return ClientRequest.from(request)
				.headers(headers -> headers.setBearerAuth(user.getIdToken().getTokenValue()))
				.build();
	}
}
