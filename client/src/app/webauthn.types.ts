// WebAuthn Type Definitions

export interface WebAuthnCredential {
  id: string;
  rawId: string;
  response: {
    authenticatorData: string;
    clientDataJSON: string;
    signature: string;
    userHandle?: string;
  };
  type: string;
  clientExtensionResults: any;
  authenticatorAttachment?: string;
}

export interface WebAuthnRegistrationCredential {
  id: string;
  rawId: string;
  response: {
    attestationObject: string;
    clientDataJSON: string;
    transports: string[];
  };
  type: string;
  clientExtensionResults: any;
  authenticatorAttachment?: string;
}

export interface WebAuthnAuthenticationResponse {
  authenticated: boolean;
  redirectUrl?: string;
}

export interface WebAuthnRegistrationRequest {
  publicKey: {
    credential: WebAuthnRegistrationCredential;
    label: string;
  };
}

export interface WebAuthnRegistrationResponse {
  success: boolean;
  message?: string;
}

export interface WebAuthnAuthenticationOptions {
  challenge: string;
  allowCredentials?: Array<{
    id: string;
    type: string;
    transports?: string[];
  }>;
  userVerification?: string;
  rpId?: string;
  timeout?: number;
}

export interface WebAuthnRegistrationOptions {
  challenge: string;
  rp: {
    id: string;
    name: string;
  };
  user: {
    id: string;
    name: string;
    displayName: string;
  };
  pubKeyCredParams: Array<{
    alg: number;
    type: string;
  }>;
  excludeCredentials?: Array<{
    id: string;
    type: string;
    transports?: string[];
  }>;
  authenticatorSelection?: {
    authenticatorAttachment?: string;
    requireResidentKey?: boolean;
    userVerification?: string;
  };
  attestation?: string;
  timeout?: number;
}
