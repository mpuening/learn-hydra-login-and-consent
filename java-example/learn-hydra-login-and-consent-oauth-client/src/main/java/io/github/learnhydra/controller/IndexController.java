package io.github.learnhydra.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class IndexController {

	@GetMapping(value = { "/", "/index.html" })
	public String index() {
		return "index";
	}
	
	@GetMapping("/login.html")
	public String login() {
		return "login";
	}

	@GetMapping("/bye")
	public String bye() {
		return "bye";
	}

}
