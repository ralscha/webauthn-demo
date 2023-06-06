import {Component, OnInit, ViewChild} from '@angular/core';
import {NavController} from '@ionic/angular';
import {HttpClient, HttpParams} from '@angular/common/http';
import {MessagesService} from '../messages.service';
import {create, parseCreationOptionsFromJSON,} from "@github/webauthn-json/browser-ponyfill";
import {PublicKeyCredentialCreationOptionsJSON} from '@github/webauthn-json/src/webauthn-json/basic/json';
import {NgModel} from "@angular/forms";

@Component({
  selector: 'app-registration',
  templateUrl: './registration.page.html',
  styleUrls: ['./registration.page.scss'],
})
export class RegistrationPage implements OnInit {
  view = 'new';
  platformAuthenticatorAvailable = false;

  @ViewChild('username') usernameInput!: NgModel;

  submitError: string | null = null;
  recoveryToken: string | null = null;

  constructor(private readonly navCtrl: NavController,
              private readonly messagesService: MessagesService,
              private readonly httpClient: HttpClient) {
  }

  ngOnInit() {
    // @ts-ignore
    if (window.PublicKeyCredential && PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable) {
      PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable()
        .then(available => this.platformAuthenticatorAvailable = available);
    }
  }

  registerNew(username: string, crossPlatform: boolean): void {
    this.register(username, null, null, crossPlatform);
  }

  registerAdd(registerAddToken: string, crossPlatform: boolean): void {
    this.register(null, registerAddToken, null, crossPlatform);
  }

  recover(recovery: string, crossPlatform: boolean): void {
    this.register(null, null, recovery, crossPlatform);
  }

  selectSegment($event: Event): void {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    this.view = ($event.target as any).value;
  }

  private async register(username: string | null,
                         registrationAddToken: string | null,
                         recovery: string | null,
                         crossPlatform: boolean): Promise<void> {
    const loading = await this.messagesService.showLoading('Starting registration ...');
    await loading.present();

    let body = new HttpParams();
    if (username) {
      body = body.set('username', username);
    } else if (registrationAddToken) {
      body = body.set('registrationAddToken', registrationAddToken);
    } else if (recovery) {
      body = body.set('recoveryToken', recovery);
    }
    body = body.set('crossPlatform', crossPlatform.toString());

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
            if (registrationAddToken) {
              this.submitError = 'addTokenInvalid';
            } else {
              this.submitError = 'recoveryTokenInvalid';
            }
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

