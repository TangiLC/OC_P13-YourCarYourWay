import {
  Component,
  OnInit,
  OnDestroy,
  SimpleChanges,
  Input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatListModule } from '@angular/material/list';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { IMessage } from '@stomp/stompjs';
import { RxStomp } from '@stomp/rx-stomp';
import { myRxStompConfig } from '../../rx-stomp.config';
import { Subscription } from 'rxjs';
import { DialogService } from '../../services/dialog.service';
import { WebsocketService } from '../../services/websocket.service';
import { DialogDTO, ChatMessageDTO, UserProfileDTO } from '../../dto';
import { UserService } from '../../services/user.service';
import {
  extractDialogDate,
  extractDialogTitle,
} from '../../utils/dialog-utils';

@Component({
  selector: 'app-websocket-dialogbox',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatListModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './websocket-dialogbox.component.html',
  styleUrls: ['./websocket-dialogbox.component.scss'],
})
export class WebsocketDialogboxComponent implements OnInit, OnDestroy {
  @Input() dialogId: number | null = null;

  currentDialog: DialogDTO | null = null;
  isConnected = false;
  connectionError: string | null = null;
  messages: ChatMessageDTO[] = [];
  newMessage = '';
  currentUser: UserProfileDTO | null = null;
  isDialogLoading = false;

  private messageQueue: { destination: string; body: any }[] = [];
  private client: RxStomp;
  private subscription: Subscription | null = null;
  private dialogCreatedSub: Subscription | null = null;
  private dialogSub: Subscription | null = null;

  constructor(
    private dialogService: DialogService,
    private websocketService: WebsocketService,
    private userService: UserService
  ) {
    this.client = new RxStomp();
    this.client.configure(myRxStompConfig);
  }

  ngOnInit() {
    this.initWebsocket();
    this.currentUser = this.userService.getCurrentUser();

    if (!this.currentUser) {
      this.userService.user$.subscribe((user) => {
        this.currentUser = user;
      });
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['dialogId']) {
      if (this.dialogId && this.isConnected) {
        this.resetMessages();
        this.subscribeToDialog();
        this.loadDialogInfo();
      } else if (this.dialogId === null) {
        this.currentDialog = null;
        this.resetMessages();
        if (this.subscription) {
          this.subscription.unsubscribe();
          this.subscription = null;
        }
      }
    }
  }

  ngOnDestroy() {
    this.disconnect();
  }

  loadDialogInfo(): void {
    if (!this.dialogId) return;
    this.isDialogLoading = true;

    this.dialogSub?.unsubscribe();
    this.dialogSub = this.dialogService.getDialogById(this.dialogId).subscribe({
      next: (dialog) => {
        this.currentDialog = dialog;
        this.isDialogLoading = false;

        if (dialog.messages && dialog.messages.length > 0) {
          this.resetMessages();

          const chatMessages: ChatMessageDTO[] = dialog.messages;

          chatMessages.sort((a, b) => {
            const timeA = a.timestamp ? new Date(a.timestamp).getTime() : 0;
            const timeB = b.timestamp ? new Date(b.timestamp).getTime() : 0;
            return timeA - timeB;
          });

          this.messages = chatMessages;
        }
      },
      error: (err) => {
        console.error(
          'Erreur lors du chargement des informations du dialogue',
          err
        );
        this.currentDialog = null;
      },
    });
  }

  private initWebsocket() {
    this.client.activate();

    this.client.connected$.subscribe(() => {
      this.isConnected = true;
      this.connectionError = null;
      this.websocketService.updateConnectionStatus(true);
      console.log('WebSocket connecté');

      this.subscribeToDialogCreated();
      if (this.dialogId) {
        this.subscribeToDialog();
        this.loadDialogInfo();
      }

      while (this.messageQueue.length) {
        const msg = this.messageQueue.shift()!;
        this.client.publish(msg);
      }
    });

    this.client.stompErrors$.subscribe((frame) => {
      this.connectionError = `Erreur STOMP: ${frame.headers['message']}`;
      console.error('Erreur STOMP:', frame);
      this.isConnected = false;
      this.websocketService.updateConnectionStatus(false);
    });

    this.client.webSocketErrors$.subscribe((event) => {
      this.connectionError = 'Erreur de connexion WebSocket';
      console.error('Erreur WebSocket:', event);
      this.isConnected = false;
      this.websocketService.updateConnectionStatus(false);
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
          this.currentDialog = payload;
          this.resetMessages();
          this.subscribeToDialog();
          this.dialogService.triggerDialogRefresh();
        } catch (e) {
          console.error('Erreur parsing dialog-created:', e);
        }
      });
  }

  sendMessage() {
    if (!this.newMessage.trim() || !this.dialogId) return;
    const payload = JSON.stringify({
      dialogId: this.dialogId,
      content: this.newMessage.trim(),
      sender: this.currentUser?.id.toString() || 0,
      type: 'CHAT',
    });
    console.log('###SEND MESSAGE:', payload);
    this.sendOrQueue('/app/chat.sendMessage', payload);
    this.newMessage = '';
  }

  private sendOrQueue(destination: string, body: any) {
    const msg = { destination, body };
    if (this.client && this.isConnected) {
      try {
        this.client.publish(msg);
      } catch (error) {
        console.error("Erreur d'envoi:", error);
        this.connectionError = `Erreur d'envoi: ${error}`;
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
          const msg: ChatMessageDTO = JSON.parse(message.body);
          msg.sender = msg.sender?.toString();
          this.messages.push(msg);

          this.messages.sort((a, b) => {
            const timeA = a.timestamp ? new Date(a.timestamp).getTime() : 0;
            const timeB = b.timestamp ? new Date(b.timestamp).getTime() : 0;
            return timeA - timeB;
          });
        } catch (e) {
          console.error('Erreur parsing message:', e);
        }
      });
  }

  private resetMessages() {
    this.messages = [];
  }

  private disconnect() {
    this.subscription?.unsubscribe();
    this.dialogCreatedSub?.unsubscribe();
    this.dialogSub?.unsubscribe();
    this.client.deactivate();
    this.isConnected = false;
    this.websocketService.updateConnectionStatus(false);
  }

  formatMessage(msg: ChatMessageDTO): string {
    console.log('****formatMessage', msg);
    switch (msg.type) {
      case 'JOIN':
        return `[CONNEXION] ${msg.sender}`;
      case 'LEAVE':
        return `[DÉCONNEXION] ${msg.sender}`;
      case 'CLOSE':
        return `[CLOSE] ${msg}`;
      case 'INFO':
        return `[INFO] ${msg}`;
      default:
        return `${msg.sender}: ${msg.content}`;
    }
  }

  getSenderName(senderId: string | undefined): string {
    if (!senderId || !this.currentDialog || !this.currentDialog.participants) {
      return 'Inconnu';
    }
    const id = parseInt(senderId, 10);
    if (isNaN(id)) return senderId;
    const participant = this.currentDialog.participants.find(
      (p) => p.id === id
    );
    return participant ? participant.firstName : 'Utilisateur ' + senderId;
  }

  get dialogTitle(): string {
    return extractDialogTitle(this.currentDialog?.topic);
  }

  get dialogDate(): string {
    return extractDialogDate(this.currentDialog?.topic);
  }

  getMessageCssClass(msg: ChatMessageDTO): string {
    if (msg.type !== 'CHAT') {
      return 'system-message';
    }
    const senderId = msg.sender?.toString?.();
    const currentId = this.currentUser?.id?.toString();

    if (senderId && currentId && senderId === currentId) {
      return 'my-message';
    }
    return 'other-message';
  }
}
