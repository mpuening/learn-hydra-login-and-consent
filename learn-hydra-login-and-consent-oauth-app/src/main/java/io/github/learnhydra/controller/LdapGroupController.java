package io.github.learnhydra.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/")
public class LdapGroupController {

	@GetMapping("/manager")
	@PreAuthorize("@groupPermissionEvaluator.hasPermission(#authentication, 'GROUP_MANAGERS')")
	public Mono<String> manager(Authentication authentication) {
		return Mono.just("redirect:/?manager=yes");
	}
	
	@GetMapping("/developer")
	@PreAuthorize("@groupPermissionEvaluator.hasPermission(#authentication, 'GROUP_DEVELOPERS')")
	public Mono<String> developer(Authentication authentication) {
		return Mono.just("redirect:/?developer=yes");
	}

}
