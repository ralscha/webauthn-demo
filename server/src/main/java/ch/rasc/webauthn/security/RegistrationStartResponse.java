package ch.rasc.webauthn.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;

public class RegistrationStartResponse {

  enum Status {
    OK, USERNAME_TAKEN, TOKEN_INVALID
  }

  enum Mode {
    NEW, ADD, RECOVERY
  }

  @JsonIgnore
  private final Mode mode;

  private final Status status;

  private final String registrationId;

  private final PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions;

  public RegistrationStartResponse(Mode mode, String registrationId,
      PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions) {
    this.mode = mode;
    this.status = Status.OK;
    this.registrationId = registrationId;
    this.publicKeyCredentialCreationOptions = publicKeyCredentialCreationOptions;
  }

  public RegistrationStartResponse(Status status) {
    this.mode = null;
    this.status = status;
    this.registrationId = null;
    this.publicKeyCredentialCreationOptions = null;
  }

  public Status getStatus() {
    return this.status;
  }

  public String getRegistrationId() {
    return this.registrationId;
  }

  public PublicKeyCredentialCreationOptions getPublicKeyCredentialCreationOptions() {
    return this.publicKeyCredentialCreationOptions;
  }

  public Mode getMode() {
    return this.mode;
  }

}