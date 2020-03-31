package io.github.learnhydra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.server.EnableRedisWebSession;

@Profile("!unittest")
@Configuration
@EnableRedisWebSession
public class SessionConfiguration {

	@Value("${spring.redis.host}")
	protected String host;

	@Value("${spring.redis.port}")
	protected int port;

	@Bean
	public LettuceConnectionFactory lettuceConnectionFactory() {
		return new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, port)); 
	}
	
}
