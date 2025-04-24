import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-generic-card',
  standalone: true,
  imports: [CommonModule, MatCardModule],
  inputs: ['title', 'content', 'width', 'height'],
  template: `
    <mat-card
      class="generic-card"
      [style.width]="width"
      [style.height]="height"
    >
      <mat-card-header>
        <mat-card-title>{{ title }}</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        {{ content }}
      </mat-card-content>
    </mat-card>
  `,
  styles: [
    `
      .generic-card {
        margin: 16px;
        padding: 16px;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
        border-radius: 8px;
        height: 200px;
        background: #999999;
      }
    `,
  ],
})
export class GenericCardComponent {
  title: string = 'Composant générique PoC';
  content: string = 'Placeholder pour un composant à développer';
  width: string = '100%';
  height: string = '100%';
}
