import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {RouterModule, Routes} from '@angular/router';
import {IonicModule} from '@ionic/angular';
import {RegistrationPage} from './registration.page';
import {provideHttpClient, withInterceptorsFromDi} from '@angular/common/http';

const routes: Routes = [
  {
    path: '',
    component: RegistrationPage
  }
];

@NgModule({
  declarations: [RegistrationPage], imports: [CommonModule,
    FormsModule,
    IonicModule,
    RouterModule.forChild(routes)], providers: [provideHttpClient(withInterceptorsFromDi())]
})
export class RegistrationPageModule {
}
