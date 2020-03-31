package io.github.learnhydra.controller;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.DefaultServerRedirectStrategy;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;

import io.github.learnhydra.service.Hydra;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/login")
public class LoginController implements ServerAuthenticationSuccessHandler {

	@Autowired
	protected Hydra hydra;

	@GetMapping
	public Mono<String> login(@RequestParam(value = "login_challenge", defaultValue = "") String challenge) {
		return challenge.isBlank() ? getLoginPage() : hydra.getLoginRequest(challenge).flatMap(loginResponse -> {
			if (loginResponse.path("skip").asBoolean(false)) {
				return hydra.acceptLoginRequest(challenge, loginResponse.path("subject").asText())
						.flatMap(acceptLoginResponse -> {
							return Mono.just("redirect:" + acceptLoginResponse.path("redirect_to").asText());
						});
			} else {
				return getLoginPage();
			}
		})
		.doOnError(e -> e.printStackTrace())
		.onErrorReturn("error");
	}

	public static Mono<String> getLoginPage() {
		return Mono.just("login");
	}

	@Override
	public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
		ServerWebExchange exchange = webFilterExchange.getExchange();
		return exchange.getFormData().flatMap(formData -> {
			String challenge = formData.getFirst("login_challenge");
			String subject = authentication.getName();
			ServerRedirectStrategy redirectStrategy = new DefaultServerRedirectStrategy();

			return hydra.acceptLoginRequest(challenge, subject).flatMap(acceptLoginResponse -> {
				String redirect = acceptLoginResponse.path("redirect_to").asText();
				URI location = !StringUtils.isEmpty(redirect) ? URI.create(redirect) : URI.create("/");
				return redirectStrategy.sendRedirect(exchange, location);
			});
		});

	}
}
