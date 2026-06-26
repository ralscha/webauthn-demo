import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MessagesService } from './messages.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  imports: [RouterOutlet],
})
export class AppComponent {
  readonly messages = inject(MessagesService);
}
