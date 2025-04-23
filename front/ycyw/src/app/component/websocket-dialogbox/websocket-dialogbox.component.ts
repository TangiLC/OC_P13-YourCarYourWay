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
import { Subscription } from 'rxjs';
import { DialogService } from '../../services/dialog.service';
import { WebsocketService } from '../../services/websocket.service';
import { DialogDTO, ChatMessageDTO, UserProfileDTO } from '../../dto';
import { UserService } from '../../services/user.service';
import {
  extractDialogDate,
  extractDialogTitle,
} from '../../utils/dialog-utils';
import {
  formatMessage as formatMessageUtil,
  getMessageCssClass as getMessageCssClassUtil,
} from '../../utils/websocket-utils';

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

  private subscription: Subscription | null = null;
  private dialogCreatedSub: Subscription | null = null;
  private dialogSub: Subscription | null = null;
  private userJoinedSub: Subscription | null = null;

  constructor(
    private dialogService: DialogService,
    private websocketService: WebsocketService,
    private userService: UserService
  ) {}

  ngOnInit() {
    this.websocketService.initWebsocket();
    this.subscribeToConnectionStatus();
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
    this.websocketService.disconnect();
    this.subscription?.unsubscribe();
    this.dialogCreatedSub?.unsubscribe();
    this.dialogSub?.unsubscribe();
    this.userJoinedSub?.unsubscribe();
  }

  private subscribeToConnectionStatus() {
    this.websocketService.connectionStatus$.subscribe((status) => {
      this.isConnected = status;

      if (status) {
        this.connectionError = null;
        this.subscribeToUpdateChannel();
        this.subscribeToDialogCreated();

        if (this.dialogId) {
          this.subscribeToDialog();
          this.loadDialogInfo();
        }
      }
    });
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

  private subscribeToDialogCreated() {
    this.dialogCreatedSub?.unsubscribe();
    this.dialogCreatedSub = this.websocketService.subscribeToDialogCreated(
      (payload) => {
        this.dialogId = payload.id;
        this.currentDialog = payload;
        this.resetMessages();
        this.subscribeToDialog();
        this.dialogService.triggerDialogRefresh();
      }
    );
  }

  sendMessage() {
    if (!this.newMessage.trim() || !this.dialogId) return;
    const payload = {
      dialogId: this.dialogId,
      content: this.newMessage.trim(),
      sender: this.currentUser?.id.toString() || '0',
      isRead: false,
      type: 'CHAT',
    };
    this.websocketService.sendMessage(payload);
    this.newMessage = '';
  }

  private subscribeToDialog() {
    if (!this.dialogId) return;
    this.subscription?.unsubscribe();

    this.subscription = this.websocketService.subscribeToDialog(
      this.dialogId,
      (msg) => {
        msg.sender = msg.sender?.toString();
        this.messages.push(msg);

        this.messages.sort((a, b) => {
          const timeA = a.timestamp ? new Date(a.timestamp).getTime() : 0;
          const timeB = b.timestamp ? new Date(b.timestamp).getTime() : 0;
          return timeA - timeB;
        });
      }
    );
  }

  private resetMessages() {
    this.messages = [];
  }

  disconnectFromDialog() {
    if (!this.dialogId || !this.isConnected) return;

    const payload = {
      dialogId: this.dialogId,
      content: "L'utilisateur s'est déconnecté",
      sender: this.currentUser?.id.toString() || '0',
      type: 'LEAVE',
    };
    this.websocketService.sendDisconnectMessage(payload);

    this.resetMessages();
    this.dialogId = null;
    this.currentDialog = null;
    if (this.subscription) {
      this.subscription.unsubscribe();
      this.subscription = null;
    }
    this.dialogService.triggerDialogRefresh();
  }

  subscribeToUpdateChannel() {
    this.websocketService.subscribeToUpdateChannel(() => {
      this.dialogService.triggerDialogRefresh();
    });
  }

  formatMessage(msg: ChatMessageDTO): string {
    return formatMessageUtil(msg);
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
    return getMessageCssClassUtil(msg, this.currentUser?.id.toString());
  }
}
