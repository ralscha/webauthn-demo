package ch.rasc.webauthn;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.rasc.webauthn.Application;
import ch.rasc.webauthn.security.AppUserDetail;

@RestController
public class TestService {

  @GetMapping("/secret")
  public String secretMessage(@AuthenticationPrincipal AppUserDetail user) {
    Application.log.debug("Secret accessed by user id {}, username {}",
        user.getAppUserId(), user.getUsername());
    return "a secret message";
  }

}
