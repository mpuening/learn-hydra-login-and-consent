package io.github.learnhydra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class OAuthResourceConfiguration {

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		http
			.csrf().disable()
			.authorizeExchange()
				.pathMatchers("/actuator/**").permitAll()
				.anyExchange().authenticated()
			.and()
				.oauth2ResourceServer().opaqueToken();
		return http.build();
	}
}
