package io.github.learnhydra.config;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

import net.minidev.json.JSONArray;

@Component
public class GroupPermissionEvaluator {

	public boolean hasPermission(Authentication authentication, Object permission) {
		if ((authentication instanceof OAuth2AuthenticationToken) && (permission instanceof String)) {
			return isInGroup((OAuth2AuthenticationToken) authentication, (String) permission);
		}
		return false;
	}

	private boolean isInGroup(OAuth2AuthenticationToken authentication, final String group) {
		AtomicBoolean result = new AtomicBoolean(false);
		Object groups = authentication.getPrincipal().getAttributes().get("groups");
		if (groups != null && groups instanceof JSONArray) {
			JSONArray array = (JSONArray)groups;
			array.forEach(g -> {
				boolean current = result.get();
				result.set(current || group.equals(g));
			});
		}
		return result.get();
	}

}
