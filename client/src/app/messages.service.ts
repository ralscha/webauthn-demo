import { Service, signal } from '@angular/core';

interface LoadingHandle {
  dismiss(): Promise<void>;
}

@Service()
export class MessagesService {
  readonly loadingMessage = signal<string | null>(null);
  readonly toastMessage = signal<string | null>(null);

  private loadingId = 0;
  private toastTimeout: ReturnType<typeof setTimeout> | null = null;

  async showLoading(message = 'Working'): Promise<LoadingHandle> {
    const id = ++this.loadingId;
    this.loadingMessage.set(message);

    return {
      dismiss: async () => {
        if (this.loadingId === id) {
          this.loadingMessage.set(null);
        }
      },
    };
  }

  async showErrorToast(message = 'Unexpected error occurred'): Promise<void> {
    if (this.toastTimeout) {
      clearTimeout(this.toastTimeout);
    }

    this.toastMessage.set(message);
    this.toastTimeout = setTimeout(() => {
      this.toastMessage.set(null);
      this.toastTimeout = null;
    }, 4000);
  }
}
