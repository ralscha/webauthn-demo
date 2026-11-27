import { DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../auth.service';
import { MessagesService } from '../messages.service';

@Component({
  selector: 'app-home',
  imports: [DatePipe],
  templateUrl: './home.page.html',
  styleUrls: ['./home.page.scss'],
})
export class HomePage {
  readonly passkeys = httpResource<{ registeredAt: string | null }[]>(() => ({ url: 'account/passkeys', withCredentials: true }));
  readonly secret = httpResource.text(() => ({ url: 'secret', withCredentials: true }));

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly messagesService = inject(MessagesService);

  async logout(): Promise<void> {
    const loading = await this.messagesService.showLoading('Signing out ...');
    try {
      await firstValueFrom(this.authService.logout());
      await this.router.navigateByUrl('/login', { replaceUrl: true });
    } catch {
      await this.messagesService.showErrorToast('Logout failed');
    } finally {
      await loading.dismiss();
    }
  }
}
