package ch.rasc.webauthn.security;

import static ch.rasc.webauthn.db.tables.AppUser.APP_USER;
import static ch.rasc.webauthn.db.tables.Credentials.CREDENTIALS;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import org.jooq.DSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;

import ch.rasc.webauthn.Application;
import ch.rasc.webauthn.security.dto.AssertionFinishRequest;
import ch.rasc.webauthn.security.dto.AssertionStartResponse;
import ch.rasc.webauthn.security.dto.RegistrationFinishRequest;
import ch.rasc.webauthn.security.dto.RegistrationStartResponse;
import ch.rasc.webauthn.security.dto.RegistrationStartResponse.Mode;
import ch.rasc.webauthn.util.Base58;
import ch.rasc.webauthn.util.BytesUtil;

@RestController
@Validated
public class AuthController {

  private final DSLContext dsl;

  private final Cache<String, RegistrationStartResponse> registrationCache;

  private final Cache<String, AssertionStartResponse> assertionCache;

  private final JooqCredentialRepository credentialRepository;

  private final RelyingParty relyingParty;

  private final SecureRandom random;

  public AuthController(DSLContext dsl, JooqCredentialRepository credentialRepository,
      RelyingParty relyingParty) {
    this.dsl = dsl;
    this.credentialRepository = credentialRepository;
    this.relyingParty = relyingParty;
    this.registrationCache = Caffeine.newBuilder().maximumSize(1000)
        .expireAfterAccess(5, TimeUnit.MINUTES).build();
    this.assertionCache = Caffeine.newBuilder().maximumSize(1000)
        .expireAfterAccess(5, TimeUnit.MINUTES).build();
    this.random = new SecureRandom();
  }

  @GetMapping("/authenticate")
  @ResponseStatus(code = HttpStatus.NO_CONTENT)
  public void authenticate() {
    // nothing here
  }

  @GetMapping("/registration-add")
  public String registrationAdd(@AuthenticationPrincipal AppUserDetail user) {
    byte[] addToken = new byte[16];
    this.random.nextBytes(addToken);

    this.dsl.update(APP_USER).set(APP_USER.REGISTRATION_ADD_START, LocalDateTime.now())
        .set(APP_USER.REGISTRATION_ADD_TOKEN, addToken)
        .where(APP_USER.ID.eq(user.getAppUserId())).execute();

    return Base58.encode(addToken);
  }

  @PostMapping("/registration/start")
  public RegistrationStartResponse registrationStart(
      @RequestParam(name = "username", required = false) String username,
      @RequestParam(name = "registrationAddToken",
          required = false) String registrationAddToken,
      @RequestParam(name = "recoveryToken", required = false) String recoveryToken) {

    long userId = -1;
    String name = null;
    Mode mode = null;

    if (username != null && !username.isEmpty()) {
      // cancel if the user is already registered
      int count = this.dsl.selectCount().from(APP_USER)
          .where(APP_USER.USERNAME.equalIgnoreCase(username)).fetchOne(0, int.class);
      if (count > 0) {
        return new RegistrationStartResponse(
            RegistrationStartResponse.Status.USERNAME_TAKEN);
      }

      userId = this.dsl
          .insertInto(APP_USER, APP_USER.USERNAME, APP_USER.REGISTRATION_START)
          .values(username, LocalDateTime.now()).returning(APP_USER.ID).fetchOne()
          .getId();
      name = username;
      mode = Mode.NEW;
    }
    else if (registrationAddToken != null && !registrationAddToken.isEmpty()) {
      byte[] registrationAddTokenDecoded = null;
      try {
        registrationAddTokenDecoded = Base58.decode(registrationAddToken);
      }
      catch (Exception e) {
        return new RegistrationStartResponse(
            RegistrationStartResponse.Status.TOKEN_INVALID);
      }

      var record = this.dsl.select(APP_USER.ID, APP_USER.USERNAME).from(APP_USER)
          .where(APP_USER.REGISTRATION_ADD_TOKEN.eq(registrationAddTokenDecoded).and(
              APP_USER.REGISTRATION_ADD_START.gt(LocalDateTime.now().minusMinutes(10))))
          .fetchOne();

      if (record == null) {
        return new RegistrationStartResponse(
            RegistrationStartResponse.Status.TOKEN_INVALID);
      }

      userId = record.get(APP_USER.ID);
      name = record.get(APP_USER.USERNAME);
      mode = Mode.ADD;
    }
    else if (recoveryToken != null && !recoveryToken.isEmpty()) {
      byte[] recoveryTokenDecoded = null;

      try {
        recoveryTokenDecoded = Base58.decode(recoveryToken);
      }
      catch (Exception e) {
        return new RegistrationStartResponse(
            RegistrationStartResponse.Status.TOKEN_INVALID);
      }

      var record = this.dsl.select(APP_USER.ID, APP_USER.USERNAME).from(APP_USER)
          .where(APP_USER.RECOVERY_TOKEN.eq(recoveryTokenDecoded)).fetchOne();

      if (record == null) {
        return new RegistrationStartResponse(
            RegistrationStartResponse.Status.TOKEN_INVALID);
      }

      userId = record.get(APP_USER.ID);
      name = record.get(APP_USER.USERNAME);
      mode = Mode.RECOVERY;

      this.dsl.deleteFrom(CREDENTIALS).where(CREDENTIALS.APP_USER_ID.eq(userId))
          .execute();
    }

    if (mode != null) {
      PublicKeyCredentialCreationOptions credentialCreation = this.relyingParty
          .startRegistration(StartRegistrationOptions.builder()
              .user(UserIdentity.builder().name(name).displayName(name)
                  .id(new ByteArray(BytesUtil.longToBytes(userId))).build())
              .build());

      byte[] registrationId = new byte[16];
      this.random.nextBytes(registrationId);
      RegistrationStartResponse startResponse = new RegistrationStartResponse(mode,
          Base64.getEncoder().encodeToString(registrationId), credentialCreation);

      this.registrationCache.put(startResponse.getRegistrationId(), startResponse);

      return startResponse;
    }

    return null;
  }

  @PostMapping("/registration/finish")
  public String registrationFinish(@RequestBody RegistrationFinishRequest finishRequest) {

    RegistrationStartResponse startResponse = this.registrationCache
        .getIfPresent(finishRequest.getRegistrationId());
    this.registrationCache.invalidate(finishRequest.getRegistrationId());

    if (startResponse != null) {
      try {
        RegistrationResult registrationResult = this.relyingParty
            .finishRegistration(FinishRegistrationOptions.builder()
                .request(startResponse.getPublicKeyCredentialCreationOptions())
                .response(finishRequest.getCredential()).build());

        UserIdentity userIdentity = startResponse.getPublicKeyCredentialCreationOptions()
            .getUser();

        long userId = BytesUtil.bytesToLong(userIdentity.getId().getBytes());
        this.credentialRepository.addCredential(userId,
            registrationResult.getKeyId().getId().getBytes(),
            registrationResult.getPublicKeyCose().getBytes(),
            finishRequest.getCredential().getResponse().getParsedAuthenticatorData()
                .getSignatureCounter());

        if (startResponse.getMode() == Mode.NEW
            || startResponse.getMode() == Mode.RECOVERY) {
          byte[] recoveryToken = new byte[16];
          this.random.nextBytes(recoveryToken);

          this.dsl.update(APP_USER).set(APP_USER.REGISTRATION_START, (LocalDateTime) null)
              .set(APP_USER.RECOVERY_TOKEN, recoveryToken).where(APP_USER.ID.eq(userId))
              .execute();

          return Base58.encode(recoveryToken);
        }

        this.dsl.update(APP_USER)
            .set(APP_USER.REGISTRATION_ADD_START, (LocalDateTime) null)
            .set(APP_USER.REGISTRATION_ADD_TOKEN, (byte[]) null)
            .where(APP_USER.ID.eq(userId)).execute();
        return "OK";
      }
      catch (RegistrationFailedException e) {
        Application.log.error("registration failed", e);
      }
    }
    else {
      Application.log.error("invalid registration finish request");
    }

    return null;
  }

  @PostMapping("/assertion/start")
  public AssertionStartResponse start(@RequestBody String username) {
    byte[] assertionId = new byte[16];
    this.random.nextBytes(assertionId);

    String assertionIdBase64 = Base64.getEncoder().encodeToString(assertionId);
    AssertionRequest assertionRequest = this.relyingParty
        .startAssertion(StartAssertionOptions.builder().username(username).build());

    AssertionStartResponse response = new AssertionStartResponse(assertionIdBase64,
        assertionRequest);

    this.assertionCache.put(response.getAssertionId(), response);
    return response;
  }

  @PostMapping("/assertion/finish")
  public boolean finish(@RequestBody AssertionFinishRequest finishRequest) {

    AssertionStartResponse startResponse = this.assertionCache
        .getIfPresent(finishRequest.getAssertionId());
    this.assertionCache.invalidate(finishRequest.getAssertionId());

    try {
      AssertionResult result = this.relyingParty.finishAssertion(
          FinishAssertionOptions.builder().request(startResponse.getAssertionRequest())
              .response(finishRequest.getCredential()).build());

      if (result.isSuccess()) {
        if (!this.credentialRepository.updateSignatureCount(result)) {
          Application.log.error(
              "Failed to update signature count for user \"{}\", credential \"{}\"",
              result.getUsername(), finishRequest.getCredential().getId());
        }

        long userId = BytesUtil.bytesToLong(result.getUserHandle().getBytes());
        var appUserRecord = this.dsl.selectFrom(APP_USER).where(APP_USER.ID.eq(userId))
            .fetchOne();

        if (appUserRecord != null) {
          AppUserDetail userDetail = new AppUserDetail(appUserRecord,
              new SimpleGrantedAuthority("USER"));
          AppUserAuthentication auth = new AppUserAuthentication(userDetail);
          SecurityContextHolder.getContext().setAuthentication(auth);
          return true;
        }
      }
    }
    catch (AssertionFailedException e) {
      Application.log.error("Assertion failed", e);
    }

    return false;
  }

}
