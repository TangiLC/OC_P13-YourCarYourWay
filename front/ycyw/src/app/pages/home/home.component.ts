import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';

import { GenericCardComponent } from '../../component/generic-card/generic-card.component';
import { DialogHistoryComponent } from '../../component/dialog-history/dialog-history.component';
import { WebsocketDialogboxComponent } from '../../component/websocket-dialogbox/websocket-dialogbox.component';

import { UserService } from '../../services/user.service';
import { UserProfileDTO } from '../../dto';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatToolbarModule,
    MatButtonModule,
    MatCardModule,
    GenericCardComponent,
    DialogHistoryComponent,
    WebsocketDialogboxComponent,
  ],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
})
export class HomeComponent implements OnInit {
  title = 'Accueil';

  user: UserProfileDTO | null = null;
  currentDialogId: number | null = null;

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.userService.fetchAndStoreCurrentUser().subscribe({
      next: (user) => {
        this.user = user;
      },
      error: (err) => {
        console.error(
          'Erreur lors de la récupération du profil utilisateur :',
          err
        );
      },
    });
  }

  onDialogSelected(dialogId: number) {
    this.currentDialogId = dialogId;
  }
}
