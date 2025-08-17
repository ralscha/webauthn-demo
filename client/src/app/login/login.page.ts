import {Component, inject, OnInit} from '@angular/core';
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
import {AuthService} from '../auth.service';
import {RouterLink} from '@angular/router';

@Component({
  selector: 'app-login',
  templateUrl: './login.page.html',
  imports: [RouterLink, IonRouterLink, IonHeader, IonToolbar, IonTitle, IonContent, IonGrid, IonRow, IonCol, IonButton]
})
export class LoginPage implements OnInit {
  private readonly navCtrl = inject(NavController);
  private readonly authService = inject(AuthService);
  private readonly messagesService = inject(MessagesService);

  conditionalMediationAvailable = false;

  async ngOnInit(): Promise<void> {
    // Check if conditional mediation is available for autofill
    this.conditionalMediationAvailable = await this.authService.isConditionalMediationAvailable().toPromise() || false;

    // Setup conditional mediation if available
    if (this.conditionalMediationAvailable) {
      this.setupConditionalMediation();
    }
  }

  /**
   * Handle sign in button click
   */
  async signIn(): Promise<void> {
    const loading = await this.messagesService.showLoading('Authenticating...');
    await loading.present();

    try {
      const redirectUrl = await this.authService.authenticateWithWebAuthn(false).toPromise();
      if (redirectUrl) {
        window.location.href = redirectUrl;
      } else {
        this.navCtrl.navigateRoot('/home', { replaceUrl: true });
      }
    } catch (error: any) {
      console.error('Authentication failed:', error);
      this.messagesService.showErrorToast(error.message || 'Authentication failed');
    } finally {
      loading.dismiss();
    }
  }

  /**
   * Setup conditional mediation for autofill
   */
  private async setupConditionalMediation(): Promise<void> {
    try {
      // Start conditional mediation in the background
      this.authService.authenticateWithWebAuthn(true).subscribe({
        next: (redirectUrl) => {
          if (redirectUrl) {
            window.location.href = redirectUrl;
          } else {
            this.navCtrl.navigateRoot('/home', { replaceUrl: true });
          }
        },
        error: (error) => {
          // Conditional mediation errors are usually due to user cancellation or no credentials
          console.log('Conditional mediation ended:', error.message);
        }
      });
    } catch (error) {
      console.log('Conditional mediation not available or failed:', error);
    }
  }
}
