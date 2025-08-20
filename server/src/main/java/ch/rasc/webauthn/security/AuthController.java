package ch.rasc.webauthn.security;

import static ch.rasc.webauthn.db.tables.AppUser.APP_USER;
import static ch.rasc.webauthn.db.tables.Credentials.CREDENTIALS;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import org.jooq.DSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
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
import com.yubico.webauthn.StartAssertionOptions.StartAssertionOptionsBuilder;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;

import ch.rasc.webauthn.Application;
import ch.rasc.webauthn.security.dto.AssertionFinishRequest;
import ch.rasc.webauthn.security.dto.AssertionStartResponse;
import ch.rasc.webauthn.security.dto.RegistrationFinishRequest;
import ch.rasc.webauthn.security.dto.RegistrationStartResponse;
import ch.rasc.webauthn.security.dto.RegistrationStartResponse.Mode;
import ch.rasc.webauthn.util.Base58;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@Validated
public class AuthController {

  private final DSLContext dsl;

  private final Cache<String, RegistrationStartResponse> registrationCache;
  private final Cache<String, Long> registrationUserIdCache;

  private final Cache<String, AssertionStartResponse> assertionCache;

  private final JooqCredentialRepository credentialRepository;

  private final SecurityContextRepository securityContextRepository;

  private final RelyingParty relyingParty;

  private final SecureRandom random;

  public AuthController(DSLContext dsl, JooqCredentialRepository credentialRepository,
      RelyingParty relyingParty, SecurityContextRepository securityContextRepository) {
    this.dsl = dsl;
    this.credentialRepository = credentialRepository;
    this.securityContextRepository = securityContextRepository;
    this.relyingParty = relyingParty;
    this.registrationCache = Caffeine.newBuilder().maximumSize(1000)
        .expireAfterAccess(5, TimeUnit.MINUTES).build();
    this.registrationUserIdCache = Caffeine.newBuilder().maximumSize(1000)
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

  @PostMapping("/registration/start")
  public RegistrationStartResponse registrationStart(
      @RequestParam(name = "username", required = false) String username,
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
      byte[] webAuthnIdBytes = new byte[64];
      this.random.nextBytes(webAuthnIdBytes);
      ByteArray webAuthnId = new ByteArray(webAuthnIdBytes);

      PublicKeyCredentialCreationOptions credentialCreation = this.relyingParty
          .startRegistration(StartRegistrationOptions.builder()
              .user(UserIdentity.builder().name(name).displayName(name).id(webAuthnId)
                  .build())
              .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                  .residentKey(ResidentKeyRequirement.REQUIRED)
                  .userVerification(UserVerificationRequirement.PREFERRED).build())
              .build());

      byte[] registrationId = new byte[16];
      this.random.nextBytes(registrationId);
      RegistrationStartResponse startResponse = new RegistrationStartResponse(mode,
          Base64.getEncoder().encodeToString(registrationId), credentialCreation);

      this.registrationCache.put(startResponse.getRegistrationId(), startResponse);
      this.registrationUserIdCache.put(startResponse.getRegistrationId(), userId);

      return startResponse;
    }

    return null;
  }

  @PostMapping("/registration/finish")
  public String registrationFinish(@RequestBody RegistrationFinishRequest finishRequest) {

    RegistrationStartResponse startResponse = this.registrationCache
        .getIfPresent(finishRequest.getRegistrationId());
    this.registrationCache.invalidate(finishRequest.getRegistrationId());
    Long userId = this.registrationUserIdCache
        .getIfPresent(finishRequest.getRegistrationId());
    this.registrationUserIdCache.invalidate(finishRequest.getRegistrationId());

    if (startResponse != null) {
      try {
        RegistrationResult registrationResult = this.relyingParty
            .finishRegistration(FinishRegistrationOptions.builder()
                .request(startResponse.getPublicKeyCredentialCreationOptions())
                .response(finishRequest.getCredential()).build());

        UserIdentity userIdentity = startResponse.getPublicKeyCredentialCreationOptions()
            .getUser();

        String transports = null;
        Optional<SortedSet<AuthenticatorTransport>> transportOptional = registrationResult
            .getKeyId().getTransports();
        if (transportOptional.isPresent()) {
          transports = "";
          for (AuthenticatorTransport at : transportOptional.get()) {
            if (transports.length() > 0) {
              transports += ",";
            }
            transports += at.getId();
          }
        }
        this.credentialRepository.addCredential(userId, userIdentity.getId().getBytes(),
            registrationResult.getKeyId().getId().getBytes(),
            registrationResult.getPublicKeyCose().getBytes(), transports,
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
  public AssertionStartResponse start() {
    byte[] assertionId = new byte[16];
    this.random.nextBytes(assertionId);

    String assertionIdBase64 = Base64.getEncoder().encodeToString(assertionId);
    StartAssertionOptionsBuilder userVerificationBuilder = StartAssertionOptions.builder()
        .userVerification(UserVerificationRequirement.PREFERRED);
    AssertionRequest assertionRequest = this.relyingParty
        .startAssertion(userVerificationBuilder.build());

    AssertionStartResponse response = new AssertionStartResponse(assertionIdBase64,
        assertionRequest);

    this.assertionCache.put(response.getAssertionId(), response);
    return response;
  }

  @PostMapping("/assertion/finish")
  public boolean finish(@RequestBody AssertionFinishRequest finishRequest,
      HttpServletRequest request, HttpServletResponse response) {

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

        var appUserRecord = this.dsl.select(APP_USER.asterisk()).from(APP_USER)
            .innerJoin(CREDENTIALS).onKey()
            .where(CREDENTIALS.WEBAUTHN_USER_ID
                .eq(result.getCredential().getUserHandle().getBytes()))
            .fetchOne().into(APP_USER);

        if (appUserRecord != null) {
          AppUserDetail userDetail = new AppUserDetail(appUserRecord,
              new SimpleGrantedAuthority("USER"));
          AppUserAuthentication auth = new AppUserAuthentication(userDetail);
          SecurityContextHolder.getContext().setAuthentication(auth);
          this.securityContextRepository.saveContext(SecurityContextHolder.getContext(),
              request, response);
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
