// services/websocket.service.ts
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class WebsocketService {
  private connectionStatusSubject = new BehaviorSubject<boolean>(false);
  connectionStatus$ = this.connectionStatusSubject.asObservable();

  updateConnectionStatus(isConnected: boolean): void {
    this.connectionStatusSubject.next(isConnected);
  }
}
