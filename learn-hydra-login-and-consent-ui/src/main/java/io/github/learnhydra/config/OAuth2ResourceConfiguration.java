package io.github.learnhydra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

@Configuration
@EnableReactiveMethodSecurity
public class OAuth2ResourceConfiguration {

	@Order(3)
	@Bean("oauth2ResourceSecurityWebFilterChain")
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		return http
				.securityMatcher(ServerWebExchangeMatchers.pathMatchers("/api/**", "/actuator/**"))
				.authorizeExchange()
					// For debugging purposes
					.pathMatchers("/actuator/**").permitAll()
					.anyExchange().authenticated()
				.and()
				.csrf().disable()
				.oauth2ResourceServer().opaqueToken()
				.and()
				.and()
				.build();
	}

}
