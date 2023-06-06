import {Component} from '@angular/core';
import {AuthService} from '../auth.service';
import {LoadingController, NavController, ViewDidEnter} from '@ionic/angular';
import {MessagesService} from '../messages.service';
import {HttpClient} from '@angular/common/http';
import {get, parseRequestOptionsFromJSON,} from "@github/webauthn-json/browser-ponyfill";
import {PublicKeyCredentialRequestOptionsJSON} from '@github/webauthn-json/dist/types/basic/json';

@Component({
  selector: 'app-login',
  templateUrl: './login.page.html',
  styleUrls: ['./login.page.scss'],
})
export class LoginPage implements ViewDidEnter {
  loginButtonEnabled = true;
  private abortController: AbortController | null = null;

  constructor(private readonly authService: AuthService,
              private readonly loadingCtrl: LoadingController,
              private readonly navCtrl: NavController,
              private readonly httpClient: HttpClient,
              private readonly messagesService: MessagesService) {
  }

  async ionViewDidEnter(): Promise<void> {
    if (window.PublicKeyCredential && PublicKeyCredential.isConditionalMediationAvailable) {
      PublicKeyCredential.isConditionalMediationAvailable()
        .then(async (available) => {
          this.loginButtonEnabled = !available;
          if (!this.loginButtonEnabled) {
            await this.assertionStart();
          }
        });
    }
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  async assertionStart(formValues: { username: string } | null = null): Promise<void> {
    const loading = await this.messagesService.showLoading('Initiate login ...');
    await loading.present();

    this.httpClient.post<AssertionStartResponse>('assertion/start', formValues?.username)
      .subscribe({
        next: response => this.handleAssertionStart(response),
        error: () => {
          loading.dismiss();
          this.messagesService.showErrorToast('Assertion start failed');
        },
        complete: () => loading.dismiss()
      });
  }

  forwardToRegistration() {
    if (this.abortController) {
      this.abortController.abort();
    }
    this.navCtrl.navigateForward('/registration');
  }

  private async handleAssertionStart(response: AssertionStartResponse): Promise<void> {
    const options = parseRequestOptionsFromJSON({publicKey: response.publicKeyCredentialRequestOptions})

    if (!this.loginButtonEnabled) {
      // @ts-ignore
      options.mediation = 'conditional';
    }

    this.abortController = new AbortController();
    options.signal = this.abortController.signal;
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
