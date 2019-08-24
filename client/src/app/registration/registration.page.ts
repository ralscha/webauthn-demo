import {Component} from '@angular/core';
import {NavController} from '@ionic/angular';
import {base64ToUint8Array, uint8ArrayTobase64} from '../util';
import {HttpClient, HttpParams} from '@angular/common/http';
import {MessagesService} from '../messages.service';

@Component({
  selector: 'app-registration',
  templateUrl: './registration.page.html',
  styleUrls: ['./registration.page.scss'],
})
export class RegistrationPage {
  view = 'new';

  submitError: string = null;
  recoveryToken: string = null;

  constructor(private readonly navCtrl: NavController,
              private readonly messagesService: MessagesService,
              private readonly httpClient: HttpClient) {
  }

  registerNew(username: string) {
    this.register(username, null, null);
  }

  registerAdd(registerAddToken: string) {
    this.register(null, registerAddToken, null);
  }

  recover(recovery: string) {
    this.register(null, null, recovery);
  }

  private async register(username: string, registrationAddToken: string, recovery: string) {
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

    this.httpClient.post<RegistrationStartResponse>('/registration/start', body)
      .subscribe(async (response) => {
        await loading.dismiss();
        if (response.status === 'OK') {
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
      }, () => {
        loading.dismiss();
        this.messagesService.showErrorToast('Registration failed');
      }, () => loading.dismiss());
  }

  private async createCredentials(response) {
    const createOptions = response.publicKeyCredentialCreationOptions;

    createOptions.challenge = base64ToUint8Array(createOptions.challenge);
    createOptions.user.id = base64ToUint8Array(createOptions.user.id);

    if (createOptions.excludeCredentials) {
      for (const excludeCredential of createOptions.excludeCredentials) {
        excludeCredential.id = base64ToUint8Array(excludeCredential.id);
      }
    }
    // @ts-ignore
    const credential = await navigator.credentials.create({
      publicKey: createOptions
    });

    let clientExtensionResults = {};
    try {
      clientExtensionResults = credential.getClientExtensionResults();
    } catch (e) {
      console.error('getClientExtensionResults failed', e);
    }

    const credentialResponse = {
      registrationId: response.registrationId,
      credential: {
        id: credential.id,
        type: credential.type,
        clientExtensionResults,
        response: {
          attestationObject: uint8ArrayTobase64(credential.response.attestationObject),
          clientDataJSON: uint8ArrayTobase64(credential.response.clientDataJSON)
        }
      }
    };

    const loading = await this.messagesService.showLoading('Finishing registration ...');
    await loading.present();

    this.httpClient.post('/registration/finish', credentialResponse, {responseType: 'text'})
      .subscribe(recoveryToken => {
        if (recoveryToken) {
          this.recoveryToken = recoveryToken;
        } else {
          this.messagesService.showErrorToast('Registration failed');
        }
      }, () => {
        loading.dismiss();
        this.messagesService.showErrorToast('Registration failed');
      }, () => loading.dismiss());
  }

}

interface RegistrationStartResponse {
  status: 'OK' | 'USERNAME_TAKEN' | 'TOKEN_INVALID';
  registrationId?: string;
  publicKeyCredentialCreationOptions: any;
}

