import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatListModule } from '@angular/material/list';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { IMessage } from '@stomp/stompjs';
import { RxStomp } from '@stomp/rx-stomp';
import { myRxStompConfig } from '../../rx-stomp.config';
import { Subscription } from 'rxjs';

interface ChatMessage {
  sender?: string;
  content: string;
  type: string;
}

@Component({
  selector: 'app-websocket-dialogbox',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatFormFieldModule,
    MatListModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './webocket-dialogbox.component.html',
  styleUrls: ['./webocket-dialogbox.component.scss'],
})
export class WebsocketDialogboxComponent implements OnInit, OnDestroy {
  topicInput = '';
  dialogId: string | null = null;
  isConnected = false;
  connectionError: string | null = null;
  messages: ChatMessage[] = [];
  newMessage = '';
  private messageQueue: { destination: string; body: any }[] = [];
  private client: RxStomp;
  private subscription: Subscription | null = null;
  private dialogCreatedSub: Subscription | null = null;

  constructor() {
    this.client = new RxStomp();
    this.client.configure(myRxStompConfig);
  }

  ngOnInit() {
    this.initWebsocket();
  }

  ngOnDestroy() {
    this.disconnect();
  }

  private initWebsocket() {
    this.client.activate();

    this.client.connected$.subscribe(() => {
      this.isConnected = true;
      this.connectionError = null;
      console.log('WebSocket connecté');

      this.subscribeToDialogCreated();
      this.subscribeToDialog();

      while (this.messageQueue.length) {
        const msg = this.messageQueue.shift()!;
        this.client.publish(msg);
      }
    });

    this.client.stompErrors$.subscribe((frame) => {
      this.connectionError = `Erreur STOMP : ${frame.headers['message']}`;
      console.error('Erreur STOMP :', frame);
      this.isConnected = false;
    });

    this.client.webSocketErrors$.subscribe((event) => {
      this.connectionError = 'Erreur de connexion WebSocket';
      console.error('Erreur WebSocket :', event);
      this.isConnected = false;
    });
  }

  private subscribeToDialogCreated() {
    this.dialogCreatedSub?.unsubscribe();
    this.dialogCreatedSub = this.client
      .watch('/user/queue/dialog-created')
      .subscribe((message: IMessage) => {
        try {
          const payload = JSON.parse(message.body);
          this.dialogId = payload.id;
          this.topicInput = '';
          this.resetMessages();
          this.subscribeToDialog();
        } catch (e) {
          console.error('Erreur parsing dialog-created :', e);
        }
      });
  }

  createNewDialog() {
    if (!this.topicInput.trim()) return;
    this.resetMessages();
    this.sendOrQueue('/app/chat.createDialog', this.topicInput.trim());
    this.topicInput = '';
  }

  sendMessage() {
    if (!this.newMessage.trim()) return;
    const payload = JSON.stringify({
      dialogId: this.dialogId,
      content: this.newMessage.trim(),
      type: 'CHAT',
    });
    this.sendOrQueue('/app/chat.sendMessage', payload);
    this.newMessage = '';
  }

  private sendOrQueue(destination: string, body: any) {
    const msg = { destination, body };
    if (this.client && this.isConnected) {
      try {
        this.client.publish(msg);
      } catch (error) {
        console.error("Erreur d'envoi :", error);
        this.connectionError = `Erreur d'envoi : ${error}`;
      }
    } else {
      console.log("Connection non dispo, message mis en file d'attente.", msg);
      this.messageQueue.push(msg);
    }
  }

  private subscribeToDialog() {
    if (!this.dialogId) return;
    this.subscription?.unsubscribe();
    this.subscription = this.client
      .watch(`/topic/dialog/${this.dialogId}`)
      .subscribe((message: IMessage) => {
        try {
          const msg: ChatMessage = JSON.parse(message.body);
          this.messages.push(msg);
        } catch (e) {
          console.error('Erreur parsing message :', e);
        }
      });
  }

  private resetMessages() {
    this.messages = [];
  }

  private disconnect() {
    this.subscription?.unsubscribe();
    this.dialogCreatedSub?.unsubscribe();
    this.client.deactivate();
    this.isConnected = false;
  }

  formatMessage(msg: ChatMessage): string {
    switch (msg.type) {
      case 'JOIN':
        return `[CONNEXION] ${msg.sender}`;
      case 'LEAVE':
        return `[DÉCONNEXION] ${msg.sender}`;
      case 'WARNING':
      case 'INFO':
        return `[ALERTE] ${msg.content}`;
      default:
        return `${msg.sender}: ${msg.content}`;
    }
  }
}
