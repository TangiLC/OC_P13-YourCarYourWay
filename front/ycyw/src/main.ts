import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';

import { provideHttpInterceptor } from '@angular/common/http';
import { AuthInterceptor } from './app/security/auth.interceptor';

export const appHttpProviders = [
  provideHttpInterceptor(AuthInterceptor),
];

bootstrapApplication(AppComponent, appConfig).catch((err) =>
  console.error(err)
);
