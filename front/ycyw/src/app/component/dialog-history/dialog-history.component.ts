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
import { Subscription } from 'rxjs';
import { IMessage } from '@stomp/rx-stomp';
import { WebsocketService } from '../../services/websocket.service';
import {
  extractDialogDate,
  extractDialogTitle,
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
  showError: boolean = false;
  topicInput = '';
  isConnected = false;
  dialogs: DialogDTO[] = [];
  private refreshSub: Subscription | null = null;
  private connectionSub: Subscription | null = null;

  constructor(
    private dialogService: DialogService,
    private websocketService: WebsocketService
  ) {}

  ngOnInit(): void {
    if (!this.senderId) {
      console.warn('senderId manquant dans DialogHistoryComponent');
      return;
    }
    this.loadDialogs();

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
    this.dialogService.getDialogsBySender(this.senderId).subscribe({
      next: (data) => (this.dialogs = data),
      error: (err) => {
        console.error('Erreur de récupération des dialogues', err);
        this.dialogs = [];
      },
    });
  }

  selectDialog(id: number): void {
    this.dialogSelected.emit(id);
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

  getDialogsByStatus(status: string): DialogDTO[] {
    return this.dialogs.filter((dialog) => dialog.status === status);
  }
}
