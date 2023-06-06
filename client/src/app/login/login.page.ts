import {Component, OnInit} from '@angular/core';
import {AuthService} from '../auth.service';
import {LoadingController, NavController} from '@ionic/angular';
import {MessagesService} from '../messages.service';
import {HttpClient} from '@angular/common/http';
import {get, parseRequestOptionsFromJSON,} from "@github/webauthn-json/browser-ponyfill";
import {PublicKeyCredentialRequestOptionsJSON} from '@github/webauthn-json/dist/types/basic/json';

@Component({
  selector: 'app-login',
  templateUrl: './login.page.html',
  styleUrls: ['./login.page.scss'],
})
export class LoginPage implements OnInit {
  loginButtonEnabled = true;

  constructor(private readonly authService: AuthService,
              private readonly loadingCtrl: LoadingController,
              private readonly navCtrl: NavController,
              private readonly httpClient: HttpClient,
              private readonly messagesService: MessagesService) {
  }

  async ngOnInit(): Promise<void> {
    // @ts-ignore
    if (window.PublicKeyCredential && PublicKeyCredential.isConditionalMediationAvailable) {
      const isCMA = await PublicKeyCredential.isConditionalMediationAvailable();
      if (isCMA) {
        this.loginButtonEnabled = false;
        await this.assertionStart();
      }
    }
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  async assertionStart({username}: any = null): Promise<void> {
    const loading = await this.messagesService.showLoading('Initiate login ...');
    await loading.present();

    this.httpClient.post<AssertionStartResponse>('assertion/start', username)
      .subscribe({
        next: response => this.handleAssertionStart(response),
        error: () => {
          loading.dismiss();
          this.messagesService.showErrorToast('Assertion start failed');
        },
        complete: () => loading.dismiss()
      });
  }

  private async handleAssertionStart(response: AssertionStartResponse): Promise<void> {
    const options = parseRequestOptionsFromJSON({publicKey: response.publicKeyCredentialRequestOptions})
    options.mediation = 'optional';
    const credential = await get(options);

    const assertionResponse = {
      assertionId: response.assertionId,
      credential: credential
    };

    const loading = await this.messagesService.showLoading('Validating ...');
    await loading.present();

    this.httpClient.post('assertion/finish', assertionResponse, {
      withCredentials: true, responseType: 'text'
    }).subscribe({
      next: userName => {
        if (userName) {
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
