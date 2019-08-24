package ch.rasc.webauthn.security;

import java.util.Map;

import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.web.context.request.WebRequest;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // @formatter:off
       http
		.csrf().disable()
	  	.formLogin().disable()
	  	.httpBasic().disable()
	  	.logout()
	  	  .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
	  	  .deleteCookies("JSESSIONID")
	  	.and()
	  	  .authorizeRequests()
	  	    .antMatchers("/registration/*").permitAll()
	  	    .antMatchers("/assertion/*").permitAll()
	  	    .anyRequest().authenticated()
	        .and()
	          .exceptionHandling()
	            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
    // @formatter:on
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
