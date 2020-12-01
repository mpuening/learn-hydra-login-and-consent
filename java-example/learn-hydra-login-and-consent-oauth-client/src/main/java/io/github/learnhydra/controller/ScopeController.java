package io.github.learnhydra.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/")
public class ScopeController {

	@GetMapping("/openid")
	@PreAuthorize("hasAuthority('SCOPE_openid')")
	public Mono<String> openid(Authentication authentication) {
		return Mono.just("redirect:/?openid=yes");
	}
	
	@GetMapping("/offline")
	@PreAuthorize("hasAuthority('SCOPE_offline')")
	public Mono<String> offline(Authentication authentication) {
		return Mono.just("redirect:/?offline=yes");
	}

	@GetMapping("/profile-read")
	@PreAuthorize("hasAuthority('SCOPE_profile.read')")
	public Mono<String> profileRead(Authentication authentication) {
		return Mono.just("redirect:/?profile.read=yes");
	}

}
