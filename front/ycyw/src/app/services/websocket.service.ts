import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { IMessage } from '@stomp/stompjs';
import { RxStomp } from '@stomp/rx-stomp';
import { myRxStompConfig } from '../my-rx-stomp.config';
import { ChatMessageDTO } from '../dto';

@Injectable({
  providedIn: 'root',
})
export class WebsocketService {
  private connectionStatusSubject = new BehaviorSubject<boolean>(false);
  connectionStatus$ = this.connectionStatusSubject.asObservable();

  private client: RxStomp;
  private messageQueue: { destination: string; body: any }[] = [];
  private isConnected = false;

  constructor() {
    this.client = new RxStomp();
    this.client.configure(myRxStompConfig);
  }

  createDialog(topic: string) {
    this.sendOrQueue('/app/chat.createDialog', topic || '');
  }

  joinDialog(dialogId: number) {
    const payload = {
      dialogId: dialogId,
      content: 'A rejoint le dialogue'
    };
    this.sendOrQueue('/app/chat.addUser', JSON.stringify(payload));
  }

  updateConnectionStatus(isConnected: boolean): void {
    this.connectionStatusSubject.next(isConnected);
  }

  initWebsocket() {
    this.client.activate();

    this.client.connected$.subscribe(() => {
      this.isConnected = true;
      this.updateConnectionStatus(true);
      console.log('WebSocket connecté');

      // Process any queued messages
      while (this.messageQueue.length) {
        const msg = this.messageQueue.shift()!;
        this.client.publish(msg);
      }
    });

    this.client.stompErrors$.subscribe((frame) => {
      console.error('Erreur STOMP:', frame);
      this.isConnected = false;
      this.updateConnectionStatus(false);
    });

    this.client.webSocketErrors$.subscribe((event) => {
      console.error('Erreur WebSocket:', event);
      this.isConnected = false;
      this.updateConnectionStatus(false);
    });
  }

  subscribeToDialogCreated(callback: (payload: any) => void): Subscription {
    return this.client
      .watch('/user/queue/dialog-created')
      .subscribe((message: IMessage) => {
        try {
          const payload = JSON.parse(message.body);
          callback(payload);
        } catch (e) {
          console.error('Erreur parsing dialog-created:', e);
        }
      });
  }

  sendMessage(payload: any) {
    this.sendOrQueue('/app/chat.sendMessage', JSON.stringify(payload));
  }

  subscribeToDialog(
    dialogId: number,
    callback: (msg: ChatMessageDTO) => void
  ): Subscription {
    return this.client
      .watch(`/topic/dialog/${dialogId}`)
      .subscribe((message: IMessage) => {
        try {
          const msg: ChatMessageDTO = JSON.parse(message.body);
          callback(msg);
        } catch (e) {
          console.error('Erreur parsing message:', e);
        }
      });
  }

  sendDisconnectMessage(payload: any) {
    this.sendOrQueue('/app/chat.disconnect', JSON.stringify(payload));
  }

  subscribeToUpdateChannel(callback: () => void): Subscription {
    const validStatuses = ['NEW', 'PENDING', 'OPENED', 'CLOSED'];

    return this.client
      .watch('/topic/dialogs/update')
      .subscribe((message: IMessage) => {
        try {
          if (!message.body.startsWith('{')) {
            if (validStatuses.includes(message.body)) {
              console.log('REFRESH LIST');
              callback();
            }
            return;
          }
          const payload = JSON.parse(message.body);
          if (validStatuses.includes(payload.event)) {
            console.log('REFRESH LIST');
            callback();
          }
        } catch (e) {
          console.error('Erreur parsing message STOMP:', e);
        }
      });
  }

  disconnect() {
    this.client.deactivate();
    this.isConnected = false;
    this.updateConnectionStatus(false);
  }

  private sendOrQueue(destination: string, body: any) {
    const msg = { destination, body };
    if (this.isConnected) {
      try {
        this.client.publish(msg);
      } catch (error) {
        console.error("Erreur d'envoi:", error);
      }
    } else {
      console.log("Connection non dispo, message mis en file d'attente.", msg);
      this.messageQueue.push(msg);
    }
  }
}
