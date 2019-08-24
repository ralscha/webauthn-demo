package ch.rasc.webauthn.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;

public class RegistrationFinishRequest {

  private final String registrationId;

  private final PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> credential;

  @JsonCreator
  public RegistrationFinishRequest(@JsonProperty("registrationId") String registrationId,
      @JsonProperty("credential") PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> credential) {
    this.registrationId = registrationId;
    this.credential = credential;
  }

  public String getRegistrationId() {
    return this.registrationId;
  }

  public PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> getCredential() {
    return this.credential;
  }

}