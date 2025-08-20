package ch.rasc.webauthn.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;

@Configuration
public class SecurityConfig {

  @Bean
  AuthenticationManager authenticationManager() {
    return authentication -> {
      throw new AuthenticationServiceException("Cannot authenticate " + authentication);
    };
  }

  @Bean
  public DelegatingSecurityContextRepository delegatingSecurityContextRepository() {
    return new DelegatingSecurityContextRepository(
        new RequestAttributeSecurityContextRepository(),
        new HttpSessionSecurityContextRepository());
  }

  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return web -> web.ignoring().requestMatchers("/", "/assets/**", "/svg/**", "/*.br",
        "/*.gz", "/*.html", "/*.js", "/*.css");
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(CsrfConfigurer::disable)
        .securityContext(securityContext -> securityContext
            .securityContextRepository(delegatingSecurityContextRepository()))
        .logout(customizer -> {
          customizer.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler());
          customizer.deleteCookies("JSESSIONID");
        }).authorizeHttpRequests(customizer -> {
          customizer.requestMatchers("/registration/*", "/assertion/*").permitAll();
          customizer.anyRequest().authenticated();
        }).exceptionHandling(customizer -> customizer
            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
    return http.build();
  }

}
