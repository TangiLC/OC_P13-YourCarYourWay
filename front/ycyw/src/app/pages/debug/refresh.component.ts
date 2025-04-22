import { Component, OnInit, OnDestroy } from '@angular/core';
import { RxStomp } from '@stomp/rx-stomp';
import { IMessage } from '@stomp/stompjs';
import { Subscription } from 'rxjs';
import { CommonModule } from '@angular/common';


@Component({
  selector: 'app-dialog-monitor',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="monitor-container">
      <h3>Événements de dialogue</h3>
      <div class="events-log">
        <div *ngFor="let event of dialogEvents" class="event-item">
          <span class="event-type">{{ event.message }}</span>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .monitor-container {
        border: 1px solid #ccc;
        padding: 10px;
        margin: 10px 0;
        border-radius: 4px;
      }
      .events-log {
        max-height: 200px;
        overflow-y: auto;
        background-color: #f8f9fa;
        padding: 8px;
      }
      .event-item {
        padding: 4px 0;
        border-bottom: 1px solid #eee;
      }
      .timestamp {
        color: #666;
        margin-right: 10px;
        font-size: 0.8em;
      }
      .event-type {
        font-weight: 500;
      }
    `,
  ],
})
export class DialogMonitorComponent implements OnInit, OnDestroy {
  dialogEvents: { timestamp: Date; message: string }[] = [];
  private subscription: Subscription | null = null;

  constructor(private rxStompService: RxStomp) {}

  ngOnInit(): void {
    console.log('Subscribing to /topic/dialogs/refresh');
    this.subscription = this.rxStompService
      .watch('/topic/dialogs/refresh')
      .subscribe((message: IMessage) => {
        console.log('REFRESH MESSAGE RECEIVED:', message.body);
        this.dialogEvents.unshift({
          timestamp: new Date(),
          message: message.body,
        });
      });
  }

  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }
}
