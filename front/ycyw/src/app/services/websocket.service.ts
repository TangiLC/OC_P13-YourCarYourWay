import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { RxStomp, RxStompConfig } from '@stomp/rx-stomp';
import { IMessage } from '@stomp/stompjs';
import { filter, map } from 'rxjs/operators';
import { myRxStompConfig } from '../my-rx-stomp.config';

@Injectable({ providedIn: 'root' })
export class WebsocketService {
  private client: RxStomp;
  private messageQueue: { destination: string; body: any }[] = [];

  private connectionStatusSubject = new BehaviorSubject<boolean>(false);
  connectionStatus$ = this.connectionStatusSubject.asObservable();
  updateConnectionStatus(isConnected: boolean): void {
    this.connectionStatusSubject.next(isConnected);
  }

  private connectedSubject = new BehaviorSubject<boolean>(false);
  readonly connected$ = this.connectedSubject.asObservable();

  readonly stompErrors$: Observable<string>;
  readonly webSocketErrors$: Observable<Event>;

  constructor() {
    this.client = new RxStomp();
    this.client.configure(myRxStompConfig);

    this.stompErrors$ = this.client.stompErrors$.pipe(
      map((frame) => frame.headers['message'] || 'Erreur STOMP inconnue')
    );
    this.webSocketErrors$ = this.client.webSocketErrors$;

    this.client.connected$.subscribe(() => {
      this.connectedSubject.next(true);
      this.flushQueue();
    });
    this.client.stompErrors$.subscribe(() => this.connectedSubject.next(false));
    this.client.webSocketErrors$.subscribe(() =>
      this.connectedSubject.next(false)
    );

    this.client.activate();
  }

  publish(destination: string, body: any): void {
    if (this.connectedSubject.value) {
      this.client.publish({ destination, body });
    } else {
      this.messageQueue.push({ destination, body });
    }
  }

  watch(destination: string): Observable<IMessage> {
    return this.client.watch(destination);
  }

  disconnect(): void {
    this.client.deactivate();
    this.connectedSubject.next(false);
  }

  private flushQueue(): void {
    while (this.messageQueue.length) {
      const { destination, body } = this.messageQueue.shift()!;
      this.client.publish({ destination, body });
    }
  }
}
