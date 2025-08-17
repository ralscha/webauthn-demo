import {inject, Injectable} from '@angular/core';
import {Observable, of, from} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {catchError, map, tap} from 'rxjs/operators';
import { WebAuthnService } from './webauthn.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly httpClient = inject(HttpClient);
  private readonly webAuthnService = inject(WebAuthnService);

  private loggedIn = false;

  isAuthenticated(): Observable<boolean> {
    return this.httpClient.get<void>('authenticate', {
      withCredentials: true
    }).pipe(
      tap(() => this.loggedIn = true),
      map(() => true),
      catchError(() => {
        this.loggedIn = false;
        return of(false);
      })
    );
  }

  logout(): Observable<void> {
    return this.httpClient.get<void>('logout', {
      withCredentials: true
    }).pipe(tap(() => this.loggedIn = false));
  }

  isLoggedIn(): boolean {
    return this.loggedIn;
  }

  /**
   * Authenticate using WebAuthn
   */
  authenticateWithWebAuthn(useConditionalMediation = false): Observable<string> {
    return from(this.webAuthnService.authenticate({}, useConditionalMediation));
  }

  /**
   * Register a new WebAuthn credential
   */
  registerWebAuthn(label: string): Observable<void> {
    return from(this.webAuthnService.register({}, label));
  }

  /**
   * Check if conditional mediation is available
   */
  isConditionalMediationAvailable(): Observable<boolean> {
    return from(this.webAuthnService.isConditionalMediationAvailable());
  }

}



