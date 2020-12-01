package io.github.learnhydra.config;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class WebConfiguration {

	@Value("classpath:/static/index.html")
	private Resource indexHtmlPage;

	@Bean
	public RouterFunction<ServerResponse> router() {
		return route(GET("/"), request -> ok().bodyValue(indexHtmlPage));
	}
}
