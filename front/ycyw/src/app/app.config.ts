import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideRxStomp } from './rx-stomp.config';
import { provideAnimations } from '@angular/platform-browser/animations';
import {
  provideHttpClient,
  withInterceptorsFromDi,
  HTTP_INTERCEPTORS,
} from '@angular/common/http';

import { AuthInterceptor } from './security/auth.interceptor';
import { appRoutes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true,
    },
    provideHttpClient(withInterceptorsFromDi()),

    provideRouter(appRoutes),
    provideAnimations(),
    provideRxStomp,
  ],
};
