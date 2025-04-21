import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { RxStomp } from '@stomp/rx-stomp';
import { IMessage } from '@stomp/stompjs';
import { myRxStompConfig } from './rx-stomp.config';
import { DialogService } from './services/dialog.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit {
  title = 'ycyw';

  private client: RxStomp;

  constructor(private dialogService: DialogService) {
    this.client = new RxStomp();
    this.client.configure(myRxStompConfig);
  }

  ngOnInit(): void {
    this.client.activate();

    this.client.connected$.subscribe(() => {
      this.client.watch('/topic/dialogs/new').subscribe((message: IMessage) => {
        try {
          if (!message.body.startsWith('{')) {
            if (message.body === 'NEW') {
              this.dialogService.triggerDialogRefresh();
            }
            return;
          }

          const payload = JSON.parse(message.body);
          if (payload.event === 'NEW') {
            this.dialogService.triggerDialogRefresh();
          }
        } catch (e) {
          console.error('Erreur parsing message STOMP:', e);
        }
      });
    });

    this.client.stompErrors$.subscribe((error) => {
      console.error('STOMP Error:', error);
    });

    this.client.webSocketErrors$.subscribe((error) => {
      console.error('WebSocket Error:', error);
    });
  }
}
