import { Routes } from '@angular/router';
import { AuthGuard } from './security/auth.guard';

export const appRoutes: Routes = [
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full',
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/login/login.component').then((m) => m.LoginComponent),
  },
   {
    path: 'home',
    loadComponent: () =>
      import('./pages/home/home.component').then((m) => m.HomeComponent),
    canActivate: [AuthGuard]
  },
  {
    path: '**',
    redirectTo: 'login',
  },
];
