import { Component, OnInit, OnDestroy } from '@angular/core';
import { RxStomp } from '@stomp/rx-stomp';
import { RouterOutlet } from '@angular/router';
import { IMessage } from '@stomp/stompjs';
import { Subscription } from 'rxjs';
import { myRxStompConfig } from './my-rx-stomp.config';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit, OnDestroy {
  private client: RxStomp;
  private subConnected: Subscription | null = null;
  private subStompErr: Subscription | null = null;
  private subWSErr: Subscription | null = null;
  private subDialogUpd: Subscription | null = null;

  isConnected = false;
  connectionError: string | null = null;
  updates: string[] = [];

  constructor() {
    this.client = new RxStomp();
    this.client.configure(myRxStompConfig);
  }

  ngOnInit(): void {
    this.client.activate();

    this.subConnected = this.client.connected$.subscribe(() => {
      this.isConnected = true;
      this.connectionError = null;
      console.log('AppComponent– WS connecté');
      this.subscribeToDialogUpdates();
    });

    this.subStompErr = this.client.stompErrors$.subscribe((frame) => {
      this.connectionError = `Erreur STOMP: ${frame.headers['message']}`;
      this.isConnected = false;
      console.error('STOMP Error:', frame);
    });

    this.subWSErr = this.client.webSocketErrors$.subscribe((evt) => {
      this.connectionError = 'Erreur WebSocket';
      this.isConnected = false;
      console.error('WS Error:', evt);
    });
  }

  private subscribeToDialogUpdates(): void {
    this.subDialogUpd?.unsubscribe();
    this.subDialogUpd = this.client
      .watch('/topic/dialogs/update')
      .subscribe((msg: IMessage) => {
        console.log('Reçu sur /topic/dialogs/update →', msg.body);
        this.updates.push(msg.body);
      });
  }

  ngOnDestroy(): void {
    this.subConnected?.unsubscribe();
    this.subStompErr?.unsubscribe();
    this.subWSErr?.unsubscribe();
    this.subDialogUpd?.unsubscribe();
    this.client.deactivate();
  }
}
