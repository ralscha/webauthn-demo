package ch.rasc.webauthn;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static ch.rasc.webauthn.db.tables.Credentials.CREDENTIALS;
import java.time.OffsetDateTime;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import ch.rasc.webauthn.security.AppUserDetail;

@RestController
public class TestService {

  private final DSLContext dsl;

  public TestService(DSLContext dsl) { this.dsl = dsl; }

  public record Registration(OffsetDateTime registeredAt) {}

  @GetMapping("/account/passkeys")
  public List<Registration> passkeys(@AuthenticationPrincipal AppUserDetail user) {
    var registeredAt = DSL.field(DSL.name("registered_at"), OffsetDateTime.class);
    return this.dsl.select(registeredAt).from(CREDENTIALS)
        .where(CREDENTIALS.APP_USER_ID.eq(user.getAppUserId()))
        .orderBy(registeredAt.asc().nullsLast(), CREDENTIALS.ID)
        .fetch(r -> new Registration(r.value1()));
  }

  @GetMapping("/secret")
  public String secretMessage(@AuthenticationPrincipal AppUserDetail user) {
    Application.log.debug("Secret accessed by user id {}, username {}",
        user.getAppUserId(), user.getUsername());
    return "a secret message";
  }

}
