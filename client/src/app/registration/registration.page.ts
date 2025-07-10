import {Component, inject, ViewChild} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {MessagesService} from '../messages.service';
import {create, parseCreationOptionsFromJSON,} from "@github/webauthn-json/browser-ponyfill";
import {FormsModule, NgModel} from "@angular/forms";
// @ts-expect-error
import {PublicKeyCredentialCreationOptionsJSON} from "@github/webauthn-json/dist/types/basic/json";
import {RouterLink} from '@angular/router';
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonCol,
  IonContent,
  IonGrid,
  IonHeader,
  IonInput,
  IonItem,
  IonLabel,
  IonRow,
  IonSegment,
  IonSegmentButton,
  IonTitle,
  IonToolbar
} from "@ionic/angular/standalone";

@Component({
  selector: 'app-registration',
  templateUrl: './registration.page.html',
  styleUrls: ['./registration.page.scss'],
  imports: [FormsModule, RouterLink, IonHeader, IonToolbar, IonButtons, IonBackButton, IonTitle, IonContent, IonSegment, IonSegmentButton, IonLabel, IonGrid, IonRow, IonCol, IonItem, IonInput, IonButton]
})
export class RegistrationPage {
  view = 'new';
  @ViewChild('username') usernameInput!: NgModel;
  submitError: string | null = null;
  recoveryToken: string | null = null;
  private readonly messagesService = inject(MessagesService);
  private readonly httpClient = inject(HttpClient);

  registerNew(username: string): void {
    this.register(username, null);
  }

  recover(recovery: string): void {
    this.register(null, recovery);
  }

  selectSegment($event: Event): void {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    this.view = ($event.target as any).value;
  }

  private async register(username: string | null,
                         recovery: string | null): Promise<void> {
    const loading = await this.messagesService.showLoading('Starting registration ...');
    await loading.present();

    let body = new HttpParams();
    if (username) {
      body = body.set('username', username);
    } else if (recovery) {
      body = body.set('recoveryToken', recovery);
    }

    this.httpClient.post<RegistrationStartResponse>('registration/start', body)
      .subscribe({
        next: async (response) => {
          await loading.dismiss();
          if (response.status === 'OK') {
            this.submitError = null;
            await this.createCredentials(response);
          } else if (response.status === 'USERNAME_TAKEN') {
            this.submitError = 'usernameTaken';
          } else if (response.status === 'TOKEN_INVALID') {
            this.submitError = 'recoveryTokenInvalid';
          }
          if (this.submitError) {
            this.usernameInput.control.setErrors({serverValidationError: true});
          }
        },
        error: () => {
          loading.dismiss();
          this.messagesService.showErrorToast('Registration failed');
        },
        complete: () => loading.dismiss(),
      });
  }

  private async createCredentials(response: RegistrationStartResponse): Promise<void> {
    const options = parseCreationOptionsFromJSON({publicKey: response.publicKeyCredentialCreationOptions})
    const credential = await create(options);

    const credentialResponse = {
      registrationId: response.registrationId,
      credential
    };

    const loading = await this.messagesService.showLoading('Finishing registration ...');
    await loading.present();

    this.httpClient.post('registration/finish', credentialResponse, {responseType: 'text'})
      .subscribe({
        next: recoveryToken => {
          if (recoveryToken) {
            this.recoveryToken = recoveryToken;
          } else {
            this.messagesService.showErrorToast('Registration failed');
          }
        },
        error: () => {
          loading.dismiss();
          this.messagesService.showErrorToast('Registration failed');
        },
        complete: () => loading.dismiss()
      });
  }
}

interface RegistrationStartResponse {
  status: 'OK' | 'USERNAME_TAKEN' | 'TOKEN_INVALID';
  registrationId?: string;
  publicKeyCredentialCreationOptions: PublicKeyCredentialCreationOptionsJSON;
}

