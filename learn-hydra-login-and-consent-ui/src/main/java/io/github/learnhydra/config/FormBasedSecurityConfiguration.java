package io.github.learnhydra.config;

import java.net.URI;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.ReactiveAuthenticationManagerAdapter;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.web.server.DefaultServerRedirectStrategy;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

import io.github.learnhydra.controller.LoginController;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class FormBasedSecurityConfiguration {

	@Value("${authentication.ldap-url}")
	protected String ldapUrl;

	@Autowired
	protected LoginController loginController;

	@Order(1)
	@Bean("formBasedSecurityWebFilterChain")
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		return http
				.securityMatcher(ServerWebExchangeMatchers.pathMatchers("/login/**", "/consent/**", "/signout/**", "/logout/**"))
				.authorizeExchange()
					.pathMatchers("/login").permitAll()
					.pathMatchers("/**").authenticated()
				.and()
					.formLogin()
					.loginPage("/login")
					.authenticationSuccessHandler(loginController)
					.authenticationFailureHandler(authenticationFailureHandler())
				.and()
					.logout()
						.requiresLogout(new PathPatternParserServerWebExchangeMatcher("/logout"))
						.logoutSuccessHandler(serverLogoutSuccessHandler())
				.and()
				.build();
	}

	private ServerAuthenticationFailureHandler authenticationFailureHandler() {
		ServerRedirectStrategy redirectStrategy = new DefaultServerRedirectStrategy();
		return new ServerAuthenticationFailureHandler() {
			@Override
			public Mono<Void> onAuthenticationFailure(WebFilterExchange webFilterExchange,
					AuthenticationException exception) {
				return webFilterExchange.getExchange().getFormData().flatMap(formData -> {
					String loginChallenge = formData.getFirst("login_challenge");
					String redirect = "/login?error&login_challenge=" + loginChallenge;
					return redirectStrategy.sendRedirect(webFilterExchange.getExchange(), URI.create(redirect));
				});
			}
		};
	}

	private ServerLogoutSuccessHandler serverLogoutSuccessHandler() {
		RedirectServerLogoutSuccessHandler successHandler = new RedirectServerLogoutSuccessHandler();
		successHandler.setLogoutSuccessUrl(URI.create("/bye"));
		return successHandler;
	}

	@Bean
	public ReactiveAuthenticationManager authenticationManager() throws Exception {

		DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(ldapUrl);
		contextSource.afterPropertiesSet();

		BindAuthenticator auth = new BindAuthenticator(contextSource);
		auth.setUserDnPatterns(new String[] { "uid={0},ou=people" });
		auth.afterPropertiesSet();

		//
		// This is how we fetch the groups the person is in
		//
		DefaultLdapAuthoritiesPopulator groupsPopulator = new DefaultLdapAuthoritiesPopulator(contextSource,
				"ou=groups");
		groupsPopulator.setGroupSearchFilter("(uniqueMember={0})");
		groupsPopulator.setRolePrefix("GROUP_");

		//
		// This is how we fetch additional LDAP fields
		//
		LdapUserDetailsMapper LdapUserDetailsMapper = new ExtendedLdapUserDetailsMapper();

		LdapAuthenticationProvider ldapProvider = new LdapAuthenticationProvider(auth, groupsPopulator);
		ldapProvider.setUserDetailsContextMapper(LdapUserDetailsMapper);
		AuthenticationManager authManager = new ProviderManager(Arrays.asList(ldapProvider));

		return new ReactiveAuthenticationManagerAdapter(authManager);
	}

}
