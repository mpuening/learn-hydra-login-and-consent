package io.github.learnhydra.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class OAuthClientConfiguration {

	@Value("${application.logout-url}")
	protected String logoutUrl;

	@Order(2)
	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		return http.
				authorizeExchange()
					.pathMatchers("/login.html", "/login/**", "/logout", "/bye").permitAll()

					.pathMatchers("/favicon.ico", "/images/**", "/css/**", "/webjars/**").permitAll()

					.pathMatchers("/me").permitAll()

					// For debugging purposes (not a good practice at all)
					.pathMatchers("/actuator/**").permitAll()

					.pathMatchers("/**").authenticated()
				.and()
					.csrf()
				.and()
					.exceptionHandling()
						//.authenticationEntryPoint(new RedirectServerAuthenticationEntryPoint("/oauth2/authorization/hydra-openid"))
						//.authenticationEntryPoint(new RedirectServerAuthenticationEntryPoint("/oauth2/authorization/hydra-code"))
						.authenticationEntryPoint(new RedirectServerAuthenticationEntryPoint("/login.html"))
						.accessDeniedHandler(new ServerAccessDeniedHandler() {
								@Override
								public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
									ServerHttpResponse response = exchange.getResponse();
									response.getHeaders().setLocation(URI.create("/?accessDenied="));
									response.setStatusCode(HttpStatus.FOUND);
									return Mono.empty();
								}
						})
				.and()
					.oauth2Login()
					.authenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler("/"))
					.authenticationFailureHandler(new RedirectServerAuthenticationFailureHandler("/"))
				.and()
					.logout(logout -> {
						logout.requiresLogout(new PathPatternParserServerWebExchangeMatcher("/logout"));
						RedirectServerLogoutSuccessHandler redirect = new RedirectServerLogoutSuccessHandler();
						redirect.setLogoutSuccessUrl(URI.create(logoutUrl));
						logout.logoutSuccessHandler(redirect);
					})
				.build();
	}

}
