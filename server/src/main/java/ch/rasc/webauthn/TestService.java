package ch.rasc.webauthn;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.rasc.webauthn.security.AppUserDetail;

@RestController
public class TestService {

  @GetMapping("/secret")
  public String secretMessage(@AuthenticationPrincipal AppUserDetail user) {
    System.out.println("user id:  " + user.getAppUserId());
    System.out.println("username: " + user.getUsername());
    return "a secret message";
  }

}
