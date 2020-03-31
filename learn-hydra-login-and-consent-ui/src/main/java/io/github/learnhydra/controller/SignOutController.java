package io.github.learnhydra.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;

import io.github.learnhydra.service.Hydra;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/signout")
public class SignOutController {

	@Autowired
	protected Hydra hydra;

	@GetMapping
	public Mono<String> signout(@RequestParam(value = "logout_challenge", defaultValue = "") String challenge) {
		return challenge.isBlank() ? LoginController.getLoginPage()
				: hydra.getLogoutRequest(challenge).flatMap(consentResponse -> {
					return getConfirmLogoutPage();
				})
				.doOnError(e -> e.printStackTrace())
				.onErrorReturn("error");
	}

	public static Mono<String> getConfirmLogoutPage() {
		return Mono.just("signout");
	}

	@PostMapping
	public Mono<String> signout(ServerWebExchange exchange) {
		return exchange.getFormData().flatMap(formData -> {
			String challenge = formData.getFirst("logout_challenge");
			boolean isDeny = "No".equalsIgnoreCase(formData.getFirst("submit"));
			if (isDeny) {
				return Mono.just("redirect:/");
			} else {
				return hydra.acceptLogoutRequest(challenge).flatMap(acceptLogoutResponse -> {
					return Mono.just("redirect:/logout");
				});
			}
		});
	}
}
