package ch.rasc.webauthn.security;

import static ch.rasc.javersdemo.db.tables.AppUser.APP_USER;

import org.jooq.DSLContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;

import ch.rasc.javersdemo.db.tables.records.AppUserRecord;
import ch.rasc.webauthn.AppProperties;

@Configuration
public class SecurityConfig {

  @Bean
  PublicKeyCredentialUserEntityRepository jooqPublicKeyCredentialUserEntityRepository(
      DSLContext dsl) {
    return new JooqPublicKeyCredentialUserEntityRepository(dsl);
  }

  @Bean
  UserCredentialRepository jdbcUserCredentialRepository(DSLContext dsl) {
    return new JooqUserCredentialRepository(dsl);
  }

  @Bean
  UserDetailsService userDetailsService(DSLContext dsl) {
    return username -> {
      AppUserRecord record = dsl.selectFrom(APP_USER)
          .where(APP_USER.USERNAME.eq(username)).fetchOne();

      if (record == null) {
        throw new UsernameNotFoundException("User not found: " + username);
      }

      return new AppUserDetail(record, new SimpleGrantedAuthority("ROLE_USER"));
    };
  }

  @Bean
  SecurityFilterChain filterChain(AppProperties appProperties, HttpSecurity http)
      throws Exception {
    return http
        .authorizeHttpRequests(authz -> authz.requestMatchers("/webauthn/**").permitAll()
            .anyRequest().authenticated())
        .webAuthn(webAuthn -> webAuthn.rpName(appProperties.getRelyingPartyName())
            .rpId(appProperties.getRelyingPartyId())
            .allowedOrigins(appProperties.getRelyingPartyOrigins()))
        .build();
  }

}
