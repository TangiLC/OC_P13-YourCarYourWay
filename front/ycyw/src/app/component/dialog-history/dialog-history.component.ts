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
import { ChatMessageDTO, DialogDTO, UserProfileDTO } from '../../dto';
import { DialogService } from '../../services/dialog.service';
import {
  forkJoin,
  map,
  Observable,
  of,
  Subscription,
  interval,
  BehaviorSubject,
} from 'rxjs';

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
  pendingDialogs$: Observable<DialogDTO[]> = of([]);
  openDialogs$: Observable<DialogDTO[]> = of([]);
  closedDialogs$: Observable<DialogDTO[]> = of([]);
  private refreshSub: Subscription | null = null;
  private connectionSub: Subscription | null = null;
  private wsDialogSub: Subscription | null = null;
  private intervalSub: Subscription | null = null;
  private userSubscription: Subscription | null = null;
  private currentUser: UserProfileDTO | null = null;

  private dialogsSubject = new BehaviorSubject<DialogDTO[]>([]);
  dialogs$ = this.dialogsSubject.asObservable();

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
    this.userSubscription = this.userService.user$.subscribe(user => {
      this.currentUser = user;
      this.loadDialogs();
    });

    this.pendingDialogs$ = this.dialogs$.pipe(
      map((dialogs) => dialogs.filter((d) => d.status === 'PENDING'))
    );
    this.openDialogs$ = this.dialogs$.pipe(
      map((dialogs) => dialogs.filter((d) => d.status === 'OPEN'))
    );
    this.closedDialogs$ = this.dialogs$.pipe(
      map((dialogs) => dialogs.filter((d) => d.status === 'CLOSED'))
    );

    this.refreshSub = this.dialogService.onDialogRefresh().subscribe(() => {
      this.loadDialogs();
    });
    this.connectionSub = this.websocketService.connectionStatus$.subscribe(
      (status) => {
        this.isConnected = status;
      }
    );
    this.wsDialogSub = this.websocketService.subscribeToDialog(
      this.senderId,
      (msg: ChatMessageDTO) => {
        this.loadDialogs();
      }
    );
    this.intervalSub = interval(2000).subscribe(() => {
      this.loadDialogs();
    });
  }

  ngOnDestroy(): void {
    this.refreshSub?.unsubscribe();
    this.connectionSub?.unsubscribe();
    this.wsDialogSub?.unsubscribe();
    this.intervalSub?.unsubscribe();
    this.userSubscription?.unsubscribe();
  }

  loadDialogs(): void {
    if (!this.currentUser) return;
    const user=this.currentUser
    const mine$ = this.dialogService.getDialogsBySender(user.id);

    if (user.type === 'SUPPORT') {
      const pending$ = this.dialogService.getDialogsByStatus('PENDING');
      forkJoin([mine$, pending$])
        .pipe(
          map(([mine, pending]: [DialogDTO[], DialogDTO[]]) => {
            const dialogMap: Map<number, DialogDTO> = new Map<
              number,
              DialogDTO
            >();
            mine.forEach((dialog: DialogDTO) =>
              dialogMap.set(dialog.id, dialog)
            );
            pending.forEach((dialog: DialogDTO) => {
              if (!dialogMap.has(dialog.id)) {
                dialogMap.set(dialog.id, dialog);
              }
            });
            return Array.from(dialogMap.values());
          })
        )
        .subscribe((dialogs) => {
          this.dialogsSubject.next(dialogs);
        });
    } else {
      mine$.subscribe((dialogs) => {
        this.dialogsSubject.next(dialogs);
      });
    }
  }

  selectDialog(id: number): void {
    if (!this.currentUser) return;

    this.dialogService.markDialogMessagesAsRead(id, this.currentUser.id).subscribe({
      next: () => {
        this.dialogService.getDialogById(id).subscribe((updatedDialog) => {
          const currentDialogs = this.dialogsSubject.getValue();
          const updatedDialogs = currentDialogs.map((dialog) =>
            dialog.id === id ? updatedDialog : dialog
          );
          this.dialogsSubject.next(updatedDialogs);
          this.dialogSelected.emit(id);
        });
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

  getUnreadMessagesCount(dialog: DialogDTO): number {
    if (!this.currentUser || !dialog.messages) {
      return 0;
    }
    return dialog.messages.filter(
      (msg) => !msg.isRead && msg.sender !== this.currentUser!.id.toString()
    ).length;
  }
}
