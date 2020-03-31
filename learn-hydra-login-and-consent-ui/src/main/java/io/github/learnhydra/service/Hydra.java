package io.github.learnhydra.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.learnhydra.config.ExtendedLdapUserDetailsMapper.ExtendedLdapUserDetails;
import reactor.core.publisher.Mono;

@Service
public class Hydra {

	@Value("${hydra.admin-private-url}")
	protected String hydraUrl;

	@Autowired
	protected WebClient.Builder webClientBuilder;

	@Autowired
	protected ObjectMapper objectMapper;

	public Mono<JsonNode> getLoginRequest(String challenge) {
		return get("login", challenge);
	}

	public Mono<JsonNode> acceptLoginRequest(String challenge, String subject) {
		ObjectNode body = objectMapper.createObjectNode();
		body.put("subject", subject);
		body.put("remember", false);
		body.put("remember_for", 3600);
		return put("login", "accept", challenge, body);
	}

	public Mono<JsonNode> rejectLoginRequest(String challenge, String errorMessage) {
		ObjectNode body = objectMapper.createObjectNode();
		body.put("error", "invalid_request");
		body.put("error_description", errorMessage);
		return put("login", "reject", challenge, body);
	}

	public Mono<JsonNode> getConsentRequest(String challenge) {
		return get("consent", challenge);
	}

	public Mono<JsonNode> acceptConsentRequest(String challenge, JsonNode consentResponse) {
		ObjectNode body = objectMapper.createObjectNode();
		body.set("grant_scope", consentResponse.path("requested_scope"));
		body.set("grant_access_token_audience", consentResponse.path("requested_access_token_audience"));
		body.putObject("session");
		return put("consent", "accept", challenge, body);
	}

	public Mono<JsonNode> acceptConsentRequest(Authentication authentication, String challenge, JsonNode consentResponse, List<String> grantScopes) {
		ArrayNode scopes = objectMapper.createArrayNode();
		grantScopes.stream().forEach(scope -> {
			scopes.add(scope);
		});
		ArrayNode groups = objectMapper.createArrayNode();
		authentication.getAuthorities().forEach(authority -> {
			groups.add(authority.getAuthority());
		});
		ObjectNode body = objectMapper.createObjectNode();
		body.set("grant_scope", scopes);
		body.set("grant_access_token_audience", consentResponse.path("requested_access_token_audience"));

		//
		// Here is where we add fields to the ID Token
		//
		ObjectNode idToken = body.putObject("session").putObject("id_token");
		idToken.set("groups", groups);
		idToken.put("fullName", getFullName(authentication.getPrincipal()));
		body.put("remember", false);
		body.put("remember_for", 3600);
		return put("consent", "accept", challenge, body);
	}

	public Mono<JsonNode> rejectConsentRequest(String challenge, String errorMessage) {
		ObjectNode body = objectMapper.createObjectNode();
		body.put("error", "access_denied");
		body.put("error_description", errorMessage);
		return put("consent", "reject", challenge, body);
	}

	public Mono<JsonNode> getLogoutRequest(String challenge) {
		return get("logout", challenge);
	}

	public Mono<JsonNode> acceptLogoutRequest(String challenge) {
		return put("logout", "accept", challenge, objectMapper.createObjectNode());
	}

	public Mono<JsonNode> rejectLogoutRequest(String challenge) {
		return put("logout", "reject", challenge, objectMapper.createObjectNode());
	}

	// ==========================================

	protected Mono<JsonNode> get(String flow, String challenge) {
		String path = "/oauth2/auth/requests/" + flow;
		String param = flow + "_challenge";
		return webClientBuilder
				.baseUrl(hydraUrl)
				.build()
				.get()
				.uri(uri -> uri.path(path).queryParam(param, challenge).build())
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.flatMap(response -> response.bodyToMono(JsonNode.class));
	}

	protected Mono<JsonNode> put(String flow, String action, String challenge, JsonNode body) {

		String path = "/oauth2/auth/requests/" + flow + "/" + action;
		String param = flow + "_challenge";
		return webClientBuilder
				.baseUrl(hydraUrl)
				.build()
				.put()
				.uri(uri -> uri.path(path).queryParam(param, challenge).build())
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(BodyInserters.fromValue(body))
				.exchange()
				.flatMap(response -> response.bodyToMono(JsonNode.class));
	}

	// ==========================================

	protected String getFullName(Object principal) {
		if (principal instanceof ExtendedLdapUserDetails) {
			return ((ExtendedLdapUserDetails)principal).getFullName();
		}
		return "";
	}
}
