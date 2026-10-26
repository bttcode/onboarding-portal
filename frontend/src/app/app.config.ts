import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideOAuthClient } from 'angular-oauth2-oidc';
import { routes } from './app.routes';
import { AuthService } from './core/auth.service';
import { RUNTIME_CONFIG, RuntimeConfig } from './core/runtime-config';

export function appConfig(runtime: RuntimeConfig): ApplicationConfig {
  return {
    providers: [
      provideZoneChangeDetection({ eventCoalescing: true }),
      provideRouter(routes, withComponentInputBinding()),
      // the OAuth library attaches the access token through a DI-registered interceptor
      provideHttpClient(withInterceptorsFromDi()),
      provideAnimationsAsync(),
      provideOAuthClient({
        resourceServer: { allowedUrls: ['/api'], sendAccessToken: true },
      }),
      { provide: RUNTIME_CONFIG, useValue: runtime },
      provideAppInitializer(() => inject(AuthService).init()),
    ],
  };
}
