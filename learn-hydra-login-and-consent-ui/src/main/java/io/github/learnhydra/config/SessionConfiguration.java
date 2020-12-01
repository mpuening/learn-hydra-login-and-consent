package io.github.learnhydra.config;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.ReactiveMapSessionRepository;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.config.annotation.web.server.EnableSpringWebSession;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;

/**
 * Because we can run this Login UI on localhost along with the OAuth Client,
 * we have the constraint that they default to the same session cookie name
 * and will clobber each other's session cookie. To prevent that, we use a
 * different session cookie name for this app.
 */
@Configuration
@EnableSpringWebSession
public class SessionConfiguration {
	@Bean
	public WebSessionIdResolver webSessionIdResolver() {
		CookieWebSessionIdResolver resolver = new CookieWebSessionIdResolver();
		resolver.setCookieName("AUTH_SESSIONID");
		resolver.setCookieMaxAge(Duration.ofMinutes(5L));
		resolver.addCookieInitializer((builder) -> builder.path("/"));
		resolver.addCookieInitializer((builder) -> builder.sameSite("Strict"));
		return resolver;
	}
	
	@Bean
    public ReactiveSessionRepository<?> reactiveSessionRepository() {
        return new ReactiveMapSessionRepository(new ConcurrentHashMap<>());
    }
}
