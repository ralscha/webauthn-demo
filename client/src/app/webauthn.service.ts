import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  WebAuthnAuthenticationResponse,
  WebAuthnRegistrationResponse,
  WebAuthnAuthenticationOptions,
  WebAuthnRegistrationOptions,
  WebAuthnCredential,
  WebAuthnRegistrationCredential,
  WebAuthnRegistrationRequest
} from './webauthn.types';

@Injectable({
  providedIn: 'root'
})
export class WebAuthnService {
  private abortController: AbortController | null = null;
  private readonly http = inject(HttpClient);

  /**
   * Base64URL encoding utility
   */
  private base64UrlEncode(buffer: ArrayBuffer): string {
    const base64 = window.btoa(String.fromCharCode(...new Uint8Array(buffer)));
    return base64.replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
  }

  /**
   * Base64URL decoding utility
   */
  private base64UrlDecode(base64url: string): ArrayBuffer {
    const base64 = base64url.replace(/-/g, "+").replace(/_/g, "/");
    const binStr = window.atob(base64);
    const bin = new Uint8Array(binStr.length);
    for (let i = 0; i < binStr.length; i++) {
      bin[i] = binStr.charCodeAt(i);
    }
    return bin.buffer;
  }

  /**
   * Create a new abort signal, cancelling any previous ceremony
   */
  private newAbortSignal(): AbortSignal {
    if (this.abortController) {
      this.abortController.abort("Initiating new WebAuthn ceremony, cancelling current ceremony");
    }
    this.abortController = new AbortController();
    return this.abortController.signal;
  }

  /**
   * Check if conditional mediation is available
   */
  async isConditionalMediationAvailable(): Promise<boolean> {
    return !!(window.PublicKeyCredential && 
             window.PublicKeyCredential.isConditionalMediationAvailable && 
             await window.PublicKeyCredential.isConditionalMediationAvailable());
  }

  /**
   * Authenticate with WebAuthn
   */
  async authenticate(headers: Record<string, string> = {}, useConditionalMediation = false): Promise<string> {
    let options: WebAuthnAuthenticationOptions;
    
    try {
      const optionsResponse = await this.http.post<WebAuthnAuthenticationOptions>(`/webauthn/authenticate/options`, null, {
        headers: {
          'Content-Type': 'application/json',
          ...headers
        }
      }).toPromise();
      
      if (!optionsResponse) {
        throw new Error('Failed to get authentication options');
      }
      
      options = optionsResponse;
    } catch (err: any) {
      throw new Error(`Authentication failed. Could not fetch authentication options: ${err.message}`);
    }

    // Decode allowCredentials
    const decodedAllowCredentials = !options.allowCredentials ? [] : 
      options.allowCredentials.map((cred: any) => ({
        ...cred,
        id: this.base64UrlDecode(cred.id)
      }));

    const decodedOptions = {
      ...options,
      allowCredentials: decodedAllowCredentials,
      challenge: this.base64UrlDecode(options.challenge)
    } as PublicKeyCredentialRequestOptions;

    const credentialOptions: CredentialRequestOptions = {
      publicKey: decodedOptions,
      signal: this.newAbortSignal()
    };

    if (useConditionalMediation) {
      (credentialOptions as any).mediation = "conditional";
    }

    let credential: PublicKeyCredential;
    try {
      credential = await navigator.credentials.get(credentialOptions) as PublicKeyCredential;
    } catch (err: any) {
      throw new Error(`Authentication failed. Call to navigator.credentials.get failed: ${err.message}`);
    }

    const response = credential.response as AuthenticatorAssertionResponse;
    let userHandle: string | undefined;
    if (response.userHandle) {
      userHandle = this.base64UrlEncode(response.userHandle);
    }

    const body = {
      id: credential.id,
      rawId: this.base64UrlEncode(credential.rawId),
      response: {
        authenticatorData: this.base64UrlEncode(response.authenticatorData),
        clientDataJSON: this.base64UrlEncode(response.clientDataJSON),
        signature: this.base64UrlEncode(response.signature),
        userHandle
      },
      type: credential.type,
      clientExtensionResults: credential.getClientExtensionResults(),
      authenticatorAttachment: (credential as any).authenticatorAttachment
    };

    let authenticationResponse: WebAuthnAuthenticationResponse;
    try {
      const response = await this.http.post<WebAuthnAuthenticationResponse>(`/login/webauthn`, body, {
        headers: {
          'Content-Type': 'application/json',
          ...headers
        }
      }).toPromise();
      
      if (!response) {
        throw new Error('No response received from server');
      }
      
      authenticationResponse = response;
    } catch (err: any) {
      throw new Error(`Authentication failed. Could not process the authentication request: ${err.message}`);
    }

    if (!(authenticationResponse && authenticationResponse.authenticated && authenticationResponse.redirectUrl)) {
      throw new Error(
        `Authentication failed. Expected {"authenticated": true, "redirectUrl": "..."}, server responded with: ${JSON.stringify(authenticationResponse)}`
      );
    }

    return authenticationResponse.redirectUrl;
  }

  /**
   * Register a new credential with WebAuthn
   */
  async register(headers: Record<string, string> = {}, label?: string): Promise<void> {
    if (!label) {
      throw new Error("Error: Passkey Label is required");
    }

    let options: WebAuthnRegistrationOptions;
    try {
      const optionsResponse = await this.http.post<WebAuthnRegistrationOptions>(`/webauthn/register/options`, null, {
        headers: {
          'Content-Type': 'application/json',
          ...headers
        }
      }).toPromise();
      
      if (!optionsResponse) {
        throw new Error('Failed to get registration options');
      }
      
      options = optionsResponse;
    } catch (err: any) {
      throw new Error(`Registration failed. Could not fetch registration options: ${err.message}`);
    }

    // Decode excludeCredentials
    const decodedExcludeCredentials = !options.excludeCredentials ? [] : 
      options.excludeCredentials.map((cred: any) => ({
        ...cred,
        id: this.base64UrlDecode(cred.id)
      }));

    const decodedOptions = {
      ...options,
      user: {
        ...options.user,
        id: this.base64UrlDecode(options.user.id)
      },
      challenge: this.base64UrlDecode(options.challenge),
      excludeCredentials: decodedExcludeCredentials
    } as PublicKeyCredentialCreationOptions;

    let credentialsContainer: PublicKeyCredential;
    try {
      credentialsContainer = await navigator.credentials.create({
        publicKey: decodedOptions,
        signal: this.newAbortSignal()
      }) as PublicKeyCredential;
    } catch (err: any) {
      throw new Error(`Registration failed. Call to navigator.credentials.create failed: ${err.message}`);
    }

    const response = credentialsContainer.response as AuthenticatorAttestationResponse;
    const credential = {
      id: credentialsContainer.id,
      rawId: this.base64UrlEncode(credentialsContainer.rawId),
      response: {
        attestationObject: this.base64UrlEncode(response.attestationObject),
        clientDataJSON: this.base64UrlEncode(response.clientDataJSON),
        transports: (response as any).getTransports ? (response as any).getTransports() : []
      },
      type: credentialsContainer.type,
      clientExtensionResults: credentialsContainer.getClientExtensionResults(),
      authenticatorAttachment: (credentialsContainer as any).authenticatorAttachment
    };

    const registrationRequest: WebAuthnRegistrationRequest = {
      publicKey: {
        credential,
        label
      }
    };

    let verificationResponse: WebAuthnRegistrationResponse;
    try {
      const response = await this.http.post<WebAuthnRegistrationResponse>(`/webauthn/register`, registrationRequest, {
        headers: {
          'Content-Type': 'application/json',
          ...headers
        }
      }).toPromise();
      
      if (!response) {
        throw new Error('No response received from server');
      }
      
      verificationResponse = response;
    } catch (err: any) {
      throw new Error(`Registration failed. Could not process the registration request: ${err.message}`);
    }

    if (!(verificationResponse && verificationResponse.success)) {
      throw new Error(`Registration failed. Server responded with: ${JSON.stringify(verificationResponse)}`);
    }
  }

  /**
   * Setup authentication with error handling and redirect
   */
  async authenticateOrError(headers: Record<string, string> = {}, useConditionalMediation = false): Promise<void> {
    try {
      const redirectUrl = await this.authenticate(headers, useConditionalMediation);
      window.location.href = redirectUrl;
    } catch (err) {
      console.error(err);
      window.location.href = `/login?error`;
    }
  }
}
