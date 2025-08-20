package ch.rasc.webauthn.security;

import static ch.rasc.webauthn.db.tables.AppUser.APP_USER;

import java.time.LocalDateTime;

import org.jooq.DSLContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CleanupJob {

  private final DSLContext dsl;

  public CleanupJob(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Scheduled(cron = "0 0 * * * *")
  public void doCleanup() {
    // Delete all users with a pending registration older than 10 minutes
    this.dsl.delete(APP_USER)
        .where(APP_USER.REGISTRATION_START.le(LocalDateTime.now().minusMinutes(10)))
        .execute();
  }

}
