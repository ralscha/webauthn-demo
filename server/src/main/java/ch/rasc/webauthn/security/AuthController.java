package ch.rasc.webauthn.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class AuthController {

  @GetMapping("/authenticate")
  @ResponseStatus(code = HttpStatus.NO_CONTENT)
  public void authenticate(@AuthenticationPrincipal AppUserDetail userDetail) {
    // This endpoint can be used to verify authentication status
    // The @AuthenticationPrincipal will ensure the user is authenticated
    // via WebAuthn before reaching this method
  }

  @GetMapping("/user")
  public UserInfo getCurrentUser(@AuthenticationPrincipal AppUserDetail userDetail) {
    return new UserInfo(userDetail.getUsername(), userDetail.getAppUserId());
  }

  public record UserInfo(String username, Long userId) {
  }

}
