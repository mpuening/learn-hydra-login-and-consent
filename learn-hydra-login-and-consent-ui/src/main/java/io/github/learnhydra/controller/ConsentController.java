package io.github.learnhydra.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.node.ArrayNode;

import io.github.learnhydra.service.Hydra;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/consent")
public class ConsentController {

	@Autowired
	protected Hydra hydra;

	@GetMapping
	public Mono<String> consent(@RequestParam(value = "consent_challenge", defaultValue = "") String challenge, Model model) {
		return challenge.isBlank() ? LoginController.getLoginPage()
				: hydra.getConsentRequest(challenge).flatMap(consentResponse -> {
					if (consentResponse.path("skip").asBoolean(false)) {
						return hydra.acceptConsentRequest(challenge, consentResponse).flatMap(acceptConsentResponse -> {
							return Mono.just("redirect:" + acceptConsentResponse.path("redirect_to").asText());
						});
					} else {
						String clientName = consentResponse.path("client").path("client_name").asText("");
						String clientId = consentResponse.path("client").path("client_id").asText("");
						String çlient = !clientName.isBlank() ? clientName : (!clientId.isBlank() ? clientId : "Unknown");
						List<String> scopes = new ArrayList<>();
						if (consentResponse.path("requested_scope").isArray()) {
							((ArrayNode) consentResponse.path("requested_scope")).forEach(scope -> {
								scopes.add(scope.asText());
							});
						}
						model.addAttribute("challenge", challenge);
						model.addAttribute("scopes", scopes);
						model.addAttribute("client", çlient);
						return getConsentPage();
					}
				})
				.doOnError(e -> e.printStackTrace())
				.onErrorReturn("error");
	}

	public static Mono<String> getConsentPage() {
		return Mono.just("consent");
	}

	@PostMapping
	public Mono<String> consent(Authentication authentication, ServerWebExchange exchange) {
		return exchange.getFormData().flatMap(formData -> {
			String challenge = formData.getFirst("consent_challenge");
			boolean isDeny = "Deny Access".equalsIgnoreCase(formData.getFirst("submit"));
			if (isDeny) {
				return hydra.rejectConsentRequest(challenge, "The resource owner denied the request")
						.flatMap(rejectConsentResponse -> {
							return Mono.just("redirect:" + rejectConsentResponse.path("redirect_to").asText());
						});
			} else {
				List<String> grantScopes = formData.getOrDefault("grant_scopes", Collections.emptyList());
				return hydra.getConsentRequest(challenge).flatMap(consentResponse -> {
					return hydra.acceptConsentRequest(authentication, challenge, consentResponse, grantScopes).flatMap(acceptConsentResponse -> {
						return Mono.just("redirect:" + acceptConsentResponse.path("redirect_to").asText());
					});
				});
			}
		});
	}
}
