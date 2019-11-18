package ch.rasc.webauthn.security;

import java.util.Map;

import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.web.context.request.WebRequest;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  @Bean
  @Override
  protected AuthenticationManager authenticationManager() throws Exception {
    return authentication -> {
      throw new AuthenticationServiceException("Cannot authenticate " + authentication);
    };
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.csrf().disable().logout(customizer -> {
      customizer.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler());
      customizer.deleteCookies("JSESSIONID");
    }).authorizeRequests(customizer -> {
      customizer.antMatchers("/registration/*", "/assertion/*").permitAll();
      customizer.anyRequest().authenticated();
    }).exceptionHandling(customizer -> customizer
        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
  }

  @Bean
  public ErrorAttributes errorAttributes() {
    return new DefaultErrorAttributes() {
      @Override
      public Map<String, Object> getErrorAttributes(WebRequest webRequest,
          boolean includeStackTrace) {
        return Map.of();
      }
    };
  }

}
