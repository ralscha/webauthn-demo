import {Component} from '@angular/core';
import {NavController} from '@ionic/angular';
import {MessagesService} from '../messages.service';
import {HttpClient} from '@angular/common/http';
import {get, parseRequestOptionsFromJSON,} from "@github/webauthn-json/browser-ponyfill";
import {PublicKeyCredentialRequestOptionsJSON} from '@github/webauthn-json/dist/types/basic/json';

@Component({
  selector: 'app-login',
  templateUrl: './login.page.html',
  styleUrls: ['./login.page.scss'],
})
export class LoginPage {

  constructor(private readonly navCtrl: NavController,
              private readonly httpClient: HttpClient,
              private readonly messagesService: MessagesService) {
  }

  async signIn(): Promise<void> {
    const loading = await this.messagesService.showLoading('Initiate login ...');
    await loading.present();

    this.httpClient.post<AssertionStartResponse>('assertion/start', null)
      .subscribe({
        next: response => this.handleAssertionStart(response),
        error: () => {
          loading.dismiss();
          this.messagesService.showErrorToast('Login failed');
        },
        complete: () => loading.dismiss()
      });
  }

  private async handleAssertionStart(response: AssertionStartResponse): Promise<void> {
    const options = parseRequestOptionsFromJSON({publicKey: response.publicKeyCredentialRequestOptions})
    const credential = await get(options);

    const assertionResponse = {
      assertionId: response.assertionId,
      credential: credential
    };

    const loading = await this.messagesService.showLoading('Validating ...');
    await loading.present();

    this.httpClient.post<boolean>('assertion/finish', assertionResponse, {
      withCredentials: true
    }).subscribe({
      next: ok => {
        if (ok) {
          this.navCtrl.navigateRoot('/home', {replaceUrl: true});
        } else {
          this.messagesService.showErrorToast('Login failed');
        }
      },
      error: () => {
        loading.dismiss();
        this.messagesService.showErrorToast('Login failed');
      },
      complete: () => loading.dismiss()
    });
  }
}

interface AssertionStartResponse {
  assertionId: string;
  publicKeyCredentialRequestOptions: PublicKeyCredentialRequestOptionsJSON;
}
