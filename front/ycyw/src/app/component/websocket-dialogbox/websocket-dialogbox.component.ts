import {
  Component,
  OnInit,
  OnDestroy,
  OnChanges,
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
import { Subject } from 'rxjs';
import { takeUntil, filter } from 'rxjs/operators';
import { IMessage } from '@stomp/stompjs';
import { DialogService } from '../../services/dialog.service';
import { WebsocketService } from '../../services/websocket.service';
import { UserService } from '../../services/user.service';
import { DialogDTO, ChatMessageDTO, UserProfileDTO } from '../../dto';
import {
  extractDialogDate,
  extractDialogTitle,
} from '../../utils/dialog-utils';
import {
  formatMessage as utilFormatMessage,
  getSenderName as utilGetSenderName,
  getMessageCssClass as utilGetMessageCssClass,
  sortMessages as utilSortMessages,
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
export class WebsocketDialogboxComponent
  implements OnInit, OnDestroy, OnChanges
{
  @Input() dialogId: number | null = null;

  currentDialog: DialogDTO | null = null;
  isConnected = false;
  connectionError: string | null = null;
  messages: ChatMessageDTO[] = [];
  newMessage = '';
  currentUser: UserProfileDTO | null = null;
  isDialogLoading = false;

  private destroyed$ = new Subject<void>();

  constructor(
    private dialogService: DialogService,
    private ws: WebsocketService,
    private userService: UserService
  ) {}

  formatMessage = (msg: ChatMessageDTO) => utilFormatMessage(msg);
  getSenderName = (senderId?: string) =>
    utilGetSenderName(senderId, this.currentDialog?.participants || []);
  getMessageCssClass = (msg: ChatMessageDTO) =>
    utilGetMessageCssClass(msg, this.currentUser?.id?.toString());

  ngOnInit() {
    this.currentUser = this.userService.getCurrentUser();
    this.userService.user$
      .pipe(
        takeUntil(this.destroyed$),
        filter((u) => !!u)
      )
      .subscribe((user) => (this.currentUser = user));

    this.ws.connected$.pipe(takeUntil(this.destroyed$)).subscribe((online) => {
      this.isConnected = online;
      if (online && this.dialogId) {
        this.initDialogSubscriptions();
        this.loadDialogInfo();
      }
    });

    this.ws.stompErrors$
      .pipe(takeUntil(this.destroyed$))
      .subscribe((msg) => (this.connectionError = `STOMP Error: ${msg}`));

    this.ws.webSocketErrors$
      .pipe(takeUntil(this.destroyed$))
      .subscribe(() => (this.connectionError = 'WebSocket Error'));

    this.ws
      .watch('/user/queue/dialog-created')
      .pipe(takeUntil(this.destroyed$))
      .subscribe((message) => this.onDialogCreated(message));
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['dialogId'] && !changes['dialogId'].isFirstChange()) {
      this.resetComponent();
      this.handleDialogChange(changes['dialogId'].currentValue);
    }
  }

  ngOnDestroy() {
    this.destroyed$.next();
    this.destroyed$.complete();
    this.ws.disconnect();
  }

  private handleDialogChange(newId: number | null) {
    if (newId && this.isConnected) {
      this.initDialogSubscriptions();
      this.loadDialogInfo();
    }
  }

  private initDialogSubscriptions() {
    if (!this.dialogId) return;
    this.ws
      .watch(`/topic/dialog/${this.dialogId}`)
      .pipe(takeUntil(this.destroyed$))
      .subscribe((message) => this.onMessageReceived(message));
  }

  private onDialogCreated(message: IMessage) {
    const dialog: DialogDTO = JSON.parse(message.body);
    this.dialogId = dialog.id;
    this.currentDialog = dialog;
    this.resetComponent();
    this.initDialogSubscriptions();
    this.dialogService.triggerDialogRefresh();
  }

  private onMessageReceived(message: IMessage) {
    try {
      const msg: ChatMessageDTO = JSON.parse(message.body);
      msg.sender = msg.sender?.toString();
      this.messages = utilSortMessages([...this.messages, msg]);
    } catch (e) {
      console.error('Parsing message error', e);
    }
  }

  loadDialogInfo(): void {
    if (!this.dialogId) return;
    this.isDialogLoading = true;
    this.dialogService
      .getDialogById(this.dialogId)
      .pipe(takeUntil(this.destroyed$))
      .subscribe({
        next: (dialog) => {
          this.currentDialog = dialog;
          this.isDialogLoading = false;
          if (dialog.messages?.length) {
            this.messages = utilSortMessages(dialog.messages);
          }
        },
        error: (err) => {
          console.error('Dialog load error', err);
          this.currentDialog = null;
          this.isDialogLoading = false;
        },
      });
  }

  sendMessage() {
    if (!this.newMessage.trim() || !this.dialogId) return;
    const payload = JSON.stringify({
      dialogId: this.dialogId,
      content: this.newMessage.trim(),
      sender: this.currentUser?.id.toString() || '0',
      type: 'CHAT',
    });
    this.ws.publish('/app/chat.sendMessage', payload);
    this.newMessage = '';
  }

  disconnectFromDialog() {
    if (!this.dialogId || !this.isConnected) return;
    const payload = JSON.stringify({
      dialogId: this.dialogId,
      content: "L'utilisateur s'est déconnecté",
      sender: this.currentUser?.id.toString() || '0',
      type: 'LEAVE',
    });
    this.ws.publish('/app/chat.disconnect', payload);
    this.resetComponent();
    this.dialogService.triggerDialogRefresh();
  }

  private resetComponent() {
    this.messages = [];
    this.currentDialog = null;
    this.dialogId = null;
    this.newMessage = '';
  }

  get dialogTitle(): string {
    return extractDialogTitle(this.currentDialog?.topic);
  }

  get dialogDate(): string {
    return extractDialogDate(this.currentDialog?.topic);
  }
}
