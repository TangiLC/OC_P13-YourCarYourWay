import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule, HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';

import { DialogDTO, ChatMessageDTO } from '../../dto';

@Component({
  selector: 'app-dialog-history',
  standalone: true,
  imports: [CommonModule, HttpClientModule, MatButtonModule],
  templateUrl: './dialog-history.component.html',
  styleUrls: ['./dialog-history.component.scss'],
})
export class DialogHistoryComponent implements OnInit {
  @Input() senderId!: number;
  @Output() dialogSelected = new EventEmitter<number>();

  dialogs: DialogDTO[] = [];

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    if (!this.senderId) {
      console.warn('senderId manquant dans DialogHistoryComponent');
      return;
    }

    this.http
      .get<DialogDTO[]>(`/api/dialog/sender/${this.senderId}`)
      .pipe(
        catchError((err) => {
          console.error('Erreur de récupération des dialogues', err);
          return of([]);
        })
      )
      .subscribe((data) => {
        this.dialogs = data;
      });
  }

  selectDialog(id: number) {
    this.dialogSelected.emit(id);
  }
}
