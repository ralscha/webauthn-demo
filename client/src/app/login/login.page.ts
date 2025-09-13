import {Component, inject} from '@angular/core';
import {
  IonButton,
  IonCol,
  IonContent,
  IonGrid,
  IonHeader,
  IonRouterLink,
  IonRow,
  IonTitle,
  IonToolbar,
  NavController
} from '@ionic/angular/standalone';
import {MessagesService} from '../messages.service';
import {HttpClient} from '@angular/common/http';
import {RouterLink} from '@angular/router';

@Component({
  selector: 'app-login',
  templateUrl: './login.page.html',
  styleUrls: ['./login.page.scss'],
  imports: [RouterLink, IonRouterLink, IonHeader, IonToolbar, IonTitle, IonContent, IonGrid, IonRow, IonCol, IonButton]
})
export class LoginPage {
  private readonly navCtrl = inject(NavController);
  private readonly httpClient = inject(HttpClient);
  private readonly messagesService = inject(MessagesService);


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
    const publicKey = PublicKeyCredential.parseRequestOptionsFromJSON(response.publicKeyCredentialRequestOptions);
    const cred = (await navigator.credentials.get({publicKey})) as PublicKeyCredential;
    const credential = cred.toJSON();

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
