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
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { DialogDTO } from '../../dto';
import { DialogService } from '../../services/dialog.service';
import { WebsocketService } from '../../services/websocket.service';
import {
  extractDialogTitle,
  extractDialogDate,
  groupByStatus,
  countUnread,
} from '../../utils/dialog-utils';

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

  topicInput = '';
  isConnected = false;
  dialogs: DialogDTO[] = [];
  grouped = new Map<string, DialogDTO[]>();

  private destroyed$ = new Subject<void>();

  constructor(
    private dialogService: DialogService,
    private ws: WebsocketService
  ) {}

  ngOnInit(): void {
    if (!this.senderId) {
      console.warn('DialogHistoryComponent: senderId is required');
      return;
    }

    this.loadDialogs();
    this.dialogService
      .onDialogRefresh()
      .pipe(takeUntil(this.destroyed$))
      .subscribe(() => this.loadDialogs());

    this.ws.connected$
      .pipe(takeUntil(this.destroyed$))
      .subscribe((status) => (this.isConnected = status));
  }

  ngOnDestroy(): void {
    this.destroyed$.next();
    this.destroyed$.complete();
  }

  private loadDialogs(): void {
    this.dialogService
      .getDialogsBySender(this.senderId)
      .pipe(takeUntil(this.destroyed$))
      .subscribe({
        next: (data) => {
          this.dialogs = data;
          this.grouped = groupByStatus(this.dialogs);
        },
        error: (err) => {
          console.error('Failed to load dialogs', err);
          this.dialogs = [];
          this.grouped.clear();
        },
      });
  }

  selectDialog(id: number): void {
    this.dialogSelected.emit(id);
  }

  createNewDialog(): void {
    const topic = this.topicInput.trim();
    if (!topic) return;
    this.dialogCreated.emit(topic);
    this.topicInput = '';
  }

  getDialogTitle(topic?: string | null): string {
    return extractDialogTitle(topic);
  }
  getDialogDate(topic?: string | null): string {
    return extractDialogDate(topic);
  }
  getDialogsByStatus(status: string): DialogDTO[] {
    return this.grouped.get(status) || [];
  }
  getUnreadCount(dialog: DialogDTO): number {
    return countUnread(dialog, this.senderId);
  }
}
