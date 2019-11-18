package ch.rasc.webauthn.security.dto;

public class RegistrationFinishResponse {

  enum Status {
    OK, REGISTRATION_FAILED
  }

  private final Status status;

  public RegistrationFinishResponse(Status status) {
    this.status = status;

  }

  public Status getStatus() {
    return this.status;
  }

}