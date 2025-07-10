import {inject} from '@angular/core';
import {Routes} from '@angular/router';
import {LoginPage} from './login/login.page';
import {AuthGuard} from './auth.guard';

export const routes: Routes = [
  {path: '', redirectTo: 'home', pathMatch: 'full'},
  {
    path: 'home',
    canActivate: [() => inject(AuthGuard).canActivate()],
    loadComponent: () => import('./home/home.page').then(m => m.HomePage)
  },
  {path: 'login', component: LoginPage},
  {
    path: 'registration',
    loadComponent: () => import('./registration/registration.page').then(m => m.RegistrationPage)
  }
];
