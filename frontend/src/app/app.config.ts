import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import { providePrimeNG } from 'primeng/config';

import { routes } from './app.routes';
import { SolIslaPreset } from './theme';
import { CurrentUserService } from './core/auth/current-user.service';
import { sessionExpiredInterceptor } from './core/auth/session-expired.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideAnimationsAsync(),
    // {} keeps Angular's defaults (XSRF-TOKEN cookie / X-XSRF-TOKEN header), which already match
    // Spring Security's CookieCsrfTokenRepository defaults on the backend.
    provideHttpClient(withXsrfConfiguration({}), withInterceptors([sessionExpiredInterceptor])),
    provideAppInitializer(() => inject(CurrentUserService).restoreSession()),
    providePrimeNG({
      theme: {
        preset: SolIslaPreset,
        options: {
          darkModeSelector: '.dark',
        },
      },
    }),
  ]
};
