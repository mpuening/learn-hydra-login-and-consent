package io.github.learnhydra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.ldap.query.LdapQueryBuilder.query;

import java.util.List;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

@ActiveProfiles("unittest")
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LearnHydraLoginAndConsentApplicationTests {

	@Value("${authentication.ldap-url}")
	protected String ldapUrl;

	@Autowired
	protected LdapTemplate ldapTemplate;

	@Autowired
	protected WebTestClient webTestClient;

	@Test
	void testLdapUserSearch() {
		List<String> names = ldapTemplate.search(query().where("objectclass").is("person"),
				new AttributesMapper<String>() {
					@Override
					public String mapFromAttributes(Attributes attributes) throws NamingException {
						return (String) attributes.get("cn").get();
					}
				});
		assertNotNull(names);
		assertEquals(3, names.size());
		assertEquals("Alice Aberdeen", names.get(0));
	}

	@Test
	public void testLdapGroupSearch() {
		DefaultSpringSecurityContextSource ctx = new DefaultSpringSecurityContextSource(ldapUrl);
		ctx.afterPropertiesSet();

		DefaultLdapAuthoritiesPopulator groupsPopulator = new DefaultLdapAuthoritiesPopulator(ctx, "ou=groups");
		groupsPopulator.setGroupSearchFilter("(uniqueMember={0})");
		Set<GrantedAuthority> authorities = groupsPopulator
				.getGroupMembershipRoles("uid=alice,ou=people,dc=example,dc=org", "alice");
		assertNotNull(authorities);
		assertEquals(1, authorities.size());
		authorities.forEach(authority -> {
			assertEquals("ROLE_MANAGERS", authority.getAuthority());
		});
	}

	@Disabled("Integration test: requires hydra to be running.")
	@Test
	@WithAnonymousUser
	public void testLogin() {
		assertNotNull(webTestClient);
		webTestClient
				.mutateWith(SecurityMockServerConfigurers.csrf())
				.post().uri("/login")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.accept(MediaType.APPLICATION_XHTML_XML)
				.body(BodyInserters.fromFormData("username", "bob").with("password", "bobspassword"))
				.exchange()
				.expectStatus().is3xxRedirection()
				.expectHeader().valueEquals("Location", "/");
	}
}
