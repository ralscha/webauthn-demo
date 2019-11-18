package ch.rasc.webauthn.security.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.data.PublicKeyCredentialRequestOptions;

public class AssertionStartResponse {

  private final String assertionId;

  private final PublicKeyCredentialRequestOptions publicKeyCredentialRequestOptions;

  @JsonIgnore
  private final AssertionRequest assertionRequest;

  public AssertionStartResponse(String assertionId, AssertionRequest assertionRequest) {
    this.assertionId = assertionId;
    this.publicKeyCredentialRequestOptions = assertionRequest
        .getPublicKeyCredentialRequestOptions();
    this.assertionRequest = assertionRequest;
  }

  public String getAssertionId() {
    return this.assertionId;
  }

  public PublicKeyCredentialRequestOptions getPublicKeyCredentialRequestOptions() {
    return this.publicKeyCredentialRequestOptions;
  }

  public AssertionRequest getAssertionRequest() {
    return this.assertionRequest;
  }

}
