import { httpResource } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { AuthService } from '../auth.service';
import {
  IonButton,
  IonContent,
  IonHeader,
  IonTitle,
  IonToolbar,
  NavController,
} from '@ionic/angular/standalone';

@Component({
  selector: 'app-home',
  templateUrl: './home.page.html',
  styleUrls: ['./home.page.scss'],
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonButton],
})
export class HomePage {
  readonly secret = httpResource.text(() => ({ url: 'secret', withCredentials: true }));

  private readonly authService = inject(AuthService);
  private readonly navCtrl = inject(NavController);

  async logout(): Promise<void> {
    this.authService.logout().subscribe(() => this.navCtrl.navigateRoot('/login'));
  }
}
