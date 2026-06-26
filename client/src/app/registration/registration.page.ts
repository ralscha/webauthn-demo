import { Component, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { MessagesService } from '../messages.service';
import type { PublicKeyCredentialJSON } from '../types';
import { FormField, FormRoot, form, required } from '@angular/forms/signals';
import type { FieldTree, TreeValidationResult } from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

type RegistrationView = 'new' | 'recover';

interface NewRegistrationFormModel {
  username: string;
}

interface RecoveryRegistrationFormModel {
  recoveryCode: string;
}

@Component({
  selector: 'app-registration',
  templateUrl: './registration.page.html',
  styleUrls: ['./registration.page.scss'],
  imports: [FormRoot, FormField, RouterLink],
})
export class RegistrationPage {
  readonly view = signal<RegistrationView>('new');
  readonly recoveryToken = signal<string | null>(null);

  readonly newRegistrationModel = signal<NewRegistrationFormModel>({ username: '' });
  readonly newRegistrationForm = form(
    this.newRegistrationModel,
    (path) => {
      required(path.username, { message: 'Username is required' });
    },
    {
      submission: {
        action: (field) => this.register(field().value().username, null, field.username),
      },
    },
  );

  readonly recoveryRegistrationModel = signal<RecoveryRegistrationFormModel>({ recoveryCode: '' });
  readonly recoveryRegistrationForm = form(
    this.recoveryRegistrationModel,
    (path) => {
      required(path.recoveryCode, { message: 'Recovery Code is required' });
    },
    {
      submission: {
        action: (field) => this.register(null, field().value().recoveryCode, field.recoveryCode),
      },
    },
  );

  private readonly messagesService = inject(MessagesService);
  private readonly httpClient = inject(HttpClient);

  usernameError(): string | null {
    const error = this.newRegistrationForm.username().errors()[0];
    return error?.message ?? null;
  }

  recoveryCodeError(): string | null {
    const error = this.recoveryRegistrationForm.recoveryCode().errors()[0];
    return error?.message ?? null;
  }

  selectView(value: RegistrationView): void {
    this.view.set(value);
  }

  private async register(
    username: string | null,
    recovery: string | null,
    inputField: FieldTree<string>,
  ): Promise<TreeValidationResult> {
    const loading = await this.messagesService.showLoading('Starting registration ...');

    let body = new HttpParams();
    if (username) {
      body = body.set('username', username);
    } else if (recovery) {
      body = body.set('recoveryToken', recovery);
    }

    let response: RegistrationStartResponse;
    try {
      response = await firstValueFrom(
        this.httpClient.post<RegistrationStartResponse>('registration/start', body),
      );
    } catch {
      await this.messagesService.showErrorToast('Registration failed');
      return { kind: 'registrationStartFailed', message: 'Registration failed' };
    } finally {
      await loading.dismiss();
    }

    if (response.status === 'OK') {
      if (this.isSuccessfulResponse(response)) {
        await this.createCredentials(response);
      }
      return undefined;
    }

    return {
      kind: response.status,
      message: this.registrationStatusMessage(response.status),
      fieldTree: inputField,
    };
  }

  private registrationStatusMessage(status: RegistrationStartResponse['status']): string {
    if (status === 'USERNAME_TAKEN') {
      return 'Username already registered';
    }
    if (status === 'TOKEN_INVALID') {
      return 'Recovery Code invalid';
    }
    return 'Registration failed';
  }

  private async createCredentials(response: SuccessfulRegistrationStartResponse): Promise<void> {
    let credential: PublicKeyCredentialJSON;
    try {
      const publicKey = PublicKeyCredential.parseCreationOptionsFromJSON(
        response.publicKeyCredentialCreationOptions,
      );
      const cred = (await navigator.credentials.create({
        publicKey,
      })) as PublicKeyCredential | null;
      if (!cred) {
        return;
      }
      credential = cred.toJSON();
    } catch (error) {
      if (!this.isExpectedCredentialError(error)) {
        await this.messagesService.showErrorToast('Registration failed');
      }
      return;
    }

    const credentialResponse = {
      registrationId: response.registrationId,
      credential,
    };

    const loading = await this.messagesService.showLoading('Finishing registration ...');

    try {
      const recoveryToken = await firstValueFrom(
        this.httpClient.post('registration/finish', credentialResponse, { responseType: 'text' }),
      );

      if (recoveryToken) {
        this.recoveryToken.set(recoveryToken);
      } else {
        await this.messagesService.showErrorToast('Registration failed');
      }
    } catch {
      await this.messagesService.showErrorToast('Registration failed');
    } finally {
      await loading.dismiss();
    }
  }

  private isExpectedCredentialError(error: unknown): boolean {
    return (
      error instanceof DOMException &&
      (error.name === 'AbortError' || error.name === 'NotAllowedError')
    );
  }

  private isSuccessfulResponse(
    response: RegistrationStartResponse,
  ): response is SuccessfulRegistrationStartResponse {
    return (
      response.status === 'OK' &&
      typeof response.registrationId === 'string' &&
      response.publicKeyCredentialCreationOptions !== undefined
    );
  }
}

interface RegistrationStartResponse {
  status: 'OK' | 'USERNAME_TAKEN' | 'TOKEN_INVALID';
  registrationId?: string;
  publicKeyCredentialCreationOptions?: PublicKeyCredentialCreationOptionsJSON;
}

interface SuccessfulRegistrationStartResponse extends RegistrationStartResponse {
  status: 'OK';
  registrationId: string;
  publicKeyCredentialCreationOptions: PublicKeyCredentialCreationOptionsJSON;
}
