import {Component} from '@angular/core';
import {AuthService} from '../auth.service';
import {LoadingController, NavController} from '@ionic/angular';
import {MessagesService} from '../messages.service';
import {HttpClient} from '@angular/common/http';
import {get} from '@github/webauthn-json';

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

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  async login({username}: any): Promise<void> {
    const loading = await this.messagesService.showLoading('Initiate login ...');
    await loading.present();

    this.httpClient.post<AssertionStartResponse>('assertion/start', username)
      .subscribe(response => this.handleAssertionStart(response), () => {
        loading.dismiss();
        this.messagesService.showErrorToast('Login failed');
      }, () => loading.dismiss());
  }

  private async handleAssertionStart(response: AssertionStartResponse): Promise<void> {
    const credential = await get({
      publicKey: response.publicKeyCredentialRequestOptions
    });

    try {
      // @ts-ignore
      credential.clientExtensionResults = credential.getClientExtensionResults();
    } catch (e) {
      // @ts-ignore
      credential.clientExtensionResults = {};
    }

    const assertionResponse = {
      assertionId: response.assertionId,
      credential
    };

    const loading = await this.messagesService.showLoading('Validating ...');
    await loading.present();

    this.httpClient.post<boolean>('assertion/finish', assertionResponse, {
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
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  publicKeyCredentialRequestOptions: any;
}
