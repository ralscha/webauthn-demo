import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {RouterLink} from '@angular/router';
import {firstValueFrom} from 'rxjs';
import {
  IonButton,
  IonCol,
  IonContent,
  IonGrid,
  IonHeader,
  IonInput,
  IonItem,
  IonRouterLink,
  IonRow,
  IonTitle,
  IonToolbar,
  NavController
} from '@ionic/angular/standalone';
import {MessagesService} from '../messages.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.page.html',
  styleUrls: ['./login.page.scss'],
  imports: [RouterLink, IonRouterLink, IonHeader, IonToolbar, IonTitle, IonContent, IonGrid, IonRow, IonCol, IonItem, IonInput, IonButton]
})
export class LoginPage implements OnInit, OnDestroy {
  conditionalMediationAvailable = false;

  private readonly navCtrl = inject(NavController);
  private readonly httpClient = inject(HttpClient);
  private readonly messagesService = inject(MessagesService);
  private conditionalMediationAbortController: AbortController | null = null;

  ngOnInit(): void {
    void this.startPasskeyAutofill();
  }

  ngOnDestroy(): void {
    this.abortConditionalMediation();
  }


  async signIn(): Promise<void> {
    this.abortConditionalMediation();

    const loading = await this.messagesService.showLoading('Initiate login ...');
    try {
      const response = await firstValueFrom(
          this.httpClient.post<AssertionStartResponse>('assertion/start', null)
      );
      await loading.dismiss();
      await this.handleAssertionStart(response);
    }
    catch {
      await loading.dismiss();
      await this.messagesService.showErrorToast('Login failed');
    }
  }

  private async handleAssertionStart(response: AssertionStartResponse): Promise<void> {
    try {
      const publicKey = PublicKeyCredential.parseRequestOptionsFromJSON(response.publicKeyCredentialRequestOptions);
      const credential = await navigator.credentials.get({publicKey}) as PublicKeyCredential | null;

      if (!credential) {
        return;
      }

      await this.finishAssertion(response.assertionId, credential.toJSON());
    }
    catch (error) {
      if (!this.isExpectedCredentialError(error)) {
        await this.messagesService.showErrorToast('Login failed');
      }
    }
  }

  private async startPasskeyAutofill(): Promise<void> {
    if (!window.PublicKeyCredential
        || typeof PublicKeyCredential.isConditionalMediationAvailable !== 'function') {
      return;
    }

    try {
      this.conditionalMediationAvailable =
          await PublicKeyCredential.isConditionalMediationAvailable();

      if (!this.conditionalMediationAvailable) {
        return;
      }

      const response = await firstValueFrom(
          this.httpClient.post<AssertionStartResponse>('assertion/start', null)
      );

      const publicKey = PublicKeyCredential.parseRequestOptionsFromJSON(
          response.publicKeyCredentialRequestOptions
      );

      this.conditionalMediationAbortController = new AbortController();
      const credential = await navigator.credentials.get({
        publicKey,
        mediation: 'conditional',
        signal: this.conditionalMediationAbortController.signal
      }) as PublicKeyCredential | null;
      this.conditionalMediationAbortController = null;

      if (!credential) {
        return;
      }

      await this.finishAssertion(response.assertionId, credential.toJSON());
    }
    catch (error) {
      this.conditionalMediationAbortController = null;
      if (!this.isExpectedCredentialError(error)) {
        await this.messagesService.showErrorToast('Passkey autofill failed');
      }
    }
  }

  private async finishAssertion(assertionId: string,
      credential: PublicKeyCredentialJSON): Promise<void> {
    const assertionResponse = {
      assertionId,
      credential
    };

    const loading = await this.messagesService.showLoading('Validating ...');
    await loading.present();

    try {
      const ok = await firstValueFrom(this.httpClient.post<boolean>('assertion/finish',
          assertionResponse, {
            withCredentials: true
          }));
      await loading.dismiss();

      if (ok) {
        await this.navCtrl.navigateRoot('/home', {replaceUrl: true});
      } else {
        await this.messagesService.showErrorToast('Login failed');
      }
    }
    catch {
      await loading.dismiss();
      await this.messagesService.showErrorToast('Login failed');
    }
  }

  private abortConditionalMediation(): void {
    this.conditionalMediationAbortController?.abort();
    this.conditionalMediationAbortController = null;
  }

  private isExpectedCredentialError(error: unknown): boolean {
    return error instanceof DOMException
        && (error.name === 'AbortError' || error.name === 'NotAllowedError');
  }
}

interface AssertionStartResponse {
  assertionId: string;
  publicKeyCredentialRequestOptions: PublicKeyCredentialRequestOptionsJSON;
}
