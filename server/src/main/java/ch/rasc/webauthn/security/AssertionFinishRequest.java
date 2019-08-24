package ch.rasc.webauthn.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;

public class AssertionFinishRequest {

  private final String assertionId;

  private final PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> credential;

  @JsonCreator
  public AssertionFinishRequest(@JsonProperty("assertionId") String assertionId,
      @JsonProperty("credential") PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> credential) {
    this.assertionId = assertionId;
    this.credential = credential;
  }

  public String getAssertionId() {
    return this.assertionId;
  }

  public PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> getCredential() {
    return this.credential;
  }

}