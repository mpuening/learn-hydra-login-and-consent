package io.github.learnhydra.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserInfoController {

	@GetMapping("/me")
	public Object me(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			Map<String, Object> anonymous = new HashMap<>();
			anonymous.put("anonymous", true);
			return anonymous;
		} else {
			return authentication.getPrincipal();
		}
	}
}
