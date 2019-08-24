import {Injectable} from '@angular/core';
import {LoadingController, ToastController} from '@ionic/angular';

@Injectable({
  providedIn: 'root'
})
export class MessagesService {

  constructor(private readonly toastCtrl: ToastController,
              private readonly loadingCtrl: LoadingController) {
  }

  async showLoading(message = 'Working') {
    const loading = await this.loadingCtrl.create({
      spinner: 'bubbles',
      message
    });
    await loading.present();
    return loading;
  }

  async showSuccessToast(message: string, duration = 4000) {
    const toast = await this.toastCtrl.create({
      message,
      duration,
      position: 'bottom',
      color: 'success'
    });
    await toast.present();
  }

  async showErrorToast(message = 'Unexpected error occurred') {
    const toast = await this.toastCtrl.create({
      message,
      duration: 4000,
      position: 'bottom',
      color: 'danger'
    });
    await toast.present();
  }

}
