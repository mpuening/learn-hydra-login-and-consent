package io.github.learnhydra.config;

import java.util.Collection;

import javax.naming.NamingException;

import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;

/**
 * This code is how we collect other LDAP fields like full name
 */
public class ExtendedLdapUserDetailsMapper extends LdapUserDetailsMapper {

	@Override
	public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
			Collection<? extends GrantedAuthority> authorities) {
		UserDetails userDetails = super.mapUserFromContext(ctx, username, authorities);
		ExtendedLdapUserDetails.ExtendedEssence essence = new ExtendedLdapUserDetails.ExtendedEssence(
				(LdapUserDetails) userDetails);
		try {
			essence.setFullName(ctx.getAttributes().get("cn").get().toString());
		} catch (NamingException e) {
			e.printStackTrace();
		}
		return essence.createUserDetails();
	}
	
	public static class ExtendedLdapUserDetails extends LdapUserDetailsImpl {

		private static final long serialVersionUID = 2790803481421346262L;

		private String fullName;

		public String getFullName() {
			return fullName;
		}
		
		public static class ExtendedEssence extends LdapUserDetailsImpl.Essence {

			private String fullName;
			
			public ExtendedEssence(LdapUserDetails copyMe) {
				super(copyMe);
			}

			protected LdapUserDetailsImpl createTarget() {
				return new ExtendedLdapUserDetails();
			}

			public String getFullName() {
				return fullName;
			}

			public void setFullName(String fullName) {
				this.fullName = fullName;
			}

			@Override
			public LdapUserDetails createUserDetails() {
				ExtendedLdapUserDetails userDetails = (ExtendedLdapUserDetails)super.createUserDetails();
				userDetails.fullName = this.fullName;
				return userDetails;
			}
		}
	}
}
