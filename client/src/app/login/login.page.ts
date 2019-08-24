import {Component} from '@angular/core';
import {AuthService} from '../auth.service';
import {LoadingController, NavController} from '@ionic/angular';
import {base64ToUint8Array, uint8ArrayTobase64} from '../util';
import {MessagesService} from '../messages.service';
import {HttpClient} from '@angular/common/http';

@Component({
  selector: 'app-login',
  templateUrl: './login.page.html',
  styleUrls: ['./login.page.scss'],
})
export class LoginPage {

  constructor(private readonly authService: AuthService,
              private readonly loadingCtrl: LoadingController,
              private readonly navCtrl: NavController,
              private readonly httpClient: HttpClient,
              private readonly messagesService: MessagesService) {
  }

  async login({username}) {
    const loading = await this.messagesService.showLoading('Initiate login ...');
    await loading.present();

    this.httpClient.post<AssertionStartResponse>('/assertion/start', username)
      .subscribe(response => this.handleAssertionStart(response), () => {
        loading.dismiss();
        this.messagesService.showErrorToast('Login failed');
      }, () => loading.dismiss());
  }

  private async handleAssertionStart(response) {
    const getOptions = response.publicKeyCredentialRequestOptions;
    getOptions.challenge = base64ToUint8Array(getOptions.challenge);
    getOptions.allowCredentials.forEach(listItem => listItem.id = base64ToUint8Array(listItem.id));

    // @ts-ignore
    const assertion = await navigator.credentials.get({
      publicKey: getOptions
    });

    let clientExtensionResults = {};
    try {
      clientExtensionResults = assertion.getClientExtensionResults();
    } catch (e) {
      console.error('getClientExtensionResults failed', e);
    }

    const assertionResponse = {
      assertionId: response.assertionId,
      credential: {
        id: assertion.id,
        type: assertion.type,
        clientExtensionResults,
        response: {
          authenticatorData: uint8ArrayTobase64(assertion.response.authenticatorData),
          clientDataJSON: uint8ArrayTobase64(assertion.response.clientDataJSON),
          signature: uint8ArrayTobase64(assertion.response.signature),
          userHandle: assertion.response.userHandle !== null ? uint8ArrayTobase64(assertion.response.userHandle) : null,
        }
      }
    };

    const loading = await this.messagesService.showLoading('Validating ...');
    await loading.present();

    this.httpClient.post<boolean>('/assertion/finish', assertionResponse, {
      withCredentials: true
    }).subscribe(ok => {
      if (ok) {
        this.navCtrl.navigateRoot('/home', {replaceUrl: true});
      } else {
        this.messagesService.showErrorToast('Login failed');
      }
    }, () => {
      loading.dismiss();
      this.messagesService.showErrorToast('Login failed');
    }, () => loading.dismiss());
  }
}

interface AssertionStartResponse {
  assertionId: string;
  publicKeyCredentialRequestOptions: any;
}
