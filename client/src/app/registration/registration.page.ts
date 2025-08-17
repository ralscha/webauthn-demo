import {Component, inject, viewChild} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {MessagesService} from '../messages.service';
import {AuthService} from '../auth.service';
import {FormsModule, NgModel} from "@angular/forms";
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
  styleUrl: './registration.page.css',
  imports: [FormsModule, RouterLink, IonHeader, IonToolbar, IonButtons, IonBackButton, IonTitle, IonContent, IonSegment, IonSegmentButton, IonLabel, IonGrid, IonRow, IonCol, IonItem, IonInput, IonButton]
})
export class RegistrationPage {
  view = 'new';
  readonly usernameInput = viewChild.required<NgModel>('username');
  submitError: string | null = null;
  recoveryToken: string | null = null;
  label = '';

  private readonly messagesService = inject(MessagesService);
  private readonly httpClient = inject(HttpClient);
  private readonly authService = inject(AuthService);

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

  /**
   * Register new WebAuthn credential
   */
  async registerWebAuthn(): Promise<void> {
    if (!this.label.trim()) {
      this.messagesService.showErrorToast('Please enter a label for your passkey');
      return;
    }

    if (!window.PublicKeyCredential) {
      this.messagesService.showErrorToast('WebAuthn is not supported by your browser');
      return;
    }

    const loading = await this.messagesService.showLoading('Creating passkey...');
    await loading.present();

    try {
      await this.authService.registerWebAuthn(this.label.trim()).toPromise();
      this.messagesService.showErrorToast('Passkey created successfully!');
      // Optionally redirect or show success message
      window.location.href = '/webauthn/register?success';
    } catch (error: any) {
      console.error('Registration failed:', error);
      this.messagesService.showErrorToast(error.message || 'Registration failed');
    } finally {
      loading.dismiss();
    }
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
        next: async (response: any) => {
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
            this.usernameInput().control.setErrors({serverValidationError: true});
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
    // Use the new WebAuthn service for registration
    const loading = await this.messagesService.showLoading('Creating credentials...');
    await loading.present();

    try {
      await this.authService.registerWebAuthn(`Credential-${Date.now()}`).toPromise();
      this.recoveryToken = 'Registration successful!';
    } catch (error: any) {
      console.error('Credential creation failed:', error);
      this.messagesService.showErrorToast(error.message || 'Credential creation failed');
    } finally {
      loading.dismiss();
    }
  }
}

interface RegistrationStartResponse {
  status: 'OK' | 'USERNAME_TAKEN' | 'TOKEN_INVALID';
  registrationId?: string;
  publicKeyCredentialCreationOptions: any;
}
