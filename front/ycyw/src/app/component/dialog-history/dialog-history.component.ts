import {
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  OnDestroy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule } from '@angular/forms';
import { DialogDTO } from '../../dto';
import { DialogService } from '../../services/dialog.service';
import { forkJoin, map, Observable, of, Subscription } from 'rxjs';
import { IMessage } from '@stomp/rx-stomp';
import { WebsocketService } from '../../services/websocket.service';
import {
  extractDialogDate,
  extractDialogTitle,
} from '../../utils/dialog-utils';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-dialog-history',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatIconModule,
    FormsModule,
  ],
  templateUrl: './dialog-history.component.html',
  styleUrls: ['./dialog-history.component.scss'],
})
export class DialogHistoryComponent implements OnInit, OnDestroy {
  @Input() senderId!: number;
  @Output() dialogSelected = new EventEmitter<number>();
  @Output() dialogCreated = new EventEmitter<string>();
  showError: boolean = false;
  topicInput = '';
  isConnected = false;
  dialogs$: Observable<DialogDTO[]> =of([]);
  pendingDialogs$: Observable<DialogDTO[]>=of([]);
  openDialogs$: Observable<DialogDTO[]>=of([]);
  closedDialogs$: Observable<DialogDTO[]>=of([]);
  private refreshSub: Subscription | null = null;
  private connectionSub: Subscription | null = null;

  constructor(
    private dialogService: DialogService,
    private websocketService: WebsocketService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    if (!this.senderId) {
      console.warn('senderId manquant dans DialogHistoryComponent');
      return;
    }
    this.loadDialogs();
    this.pendingDialogs$ = this.getDialogsByStatus$('PENDING');
    this.openDialogs$    = this.getDialogsByStatus$('OPEN');
    this.closedDialogs$  = this.getDialogsByStatus$('CLOSED');

    this.refreshSub = this.dialogService.onDialogRefresh().subscribe(() => {
      this.loadDialogs();
    });
    this.connectionSub = this.websocketService.connectionStatus$.subscribe(
      (status) => {
        this.isConnected = status;
      }
    );
  }

  ngOnDestroy(): void {
    this.refreshSub?.unsubscribe();
    this.connectionSub?.unsubscribe();
  }

  loadDialogs(): void {
    const user = this.userService.getCurrentUser();
    console.log('&&&User');
    const mine$ = this.dialogService.getDialogsBySender(user?.id || 0);

    if (user?.type === 'SUPPORT') {
      const pending$ = this.dialogService.getDialogsByStatus('PENDING');
      this.dialogs$ = forkJoin([mine$, pending$]).pipe(
        map(([mine, pending]) => [...mine, ...pending])
      );
    } else {
      this.dialogs$ = mine$;
    }
  }

  selectDialog(id: number): void {
    const currentUserId = this.userService.getCurrentUser()?.id;
    currentUserId &&
      this.dialogService.markDialogMessagesAsRead(id, currentUserId).subscribe({
        next: () => {
          this.dialogSelected.emit(id);
          this.loadDialogs();
        },
        error: (err) => {
          console.warn(
            `Impossible de marquer les messages du dialogue ${id} comme lus`,
            err
          );
          this.dialogSelected.emit(id);
        },
      });
  }

  createNewDialog(): void {
    if (!this.topicInput.trim()) {
      this.showError = true;
      return;
    }
    this.dialogCreated.emit(this.topicInput.trim());
    this.topicInput = '';
    this.showError = false;
  }

  getDialogTitle(topic: string | undefined | null): string {
    return extractDialogTitle(topic);
  }

  getDialogDate(topic: string | undefined | null): string {
    return extractDialogDate(topic);
  }

  private getDialogsByStatus$(status: string): Observable<DialogDTO[]> {
    return this.dialogs$.pipe(
      map(dialogs => dialogs.filter(d => d.status === status))
    );
  }

  getUnreadMessagesCount(dialog: DialogDTO): number {
    const currentUser = this.userService.getCurrentUser();
    if (!currentUser || !dialog.messages) {
      return 0;
    }
    return dialog.messages.filter(
      (msg) => !msg.isRead && msg.sender !== currentUser.id.toString()
    ).length;
  }
}
