import { InjectionToken } from '@angular/core';

/** Environment-specific settings, served as /config.json next to the app. */
export interface RuntimeConfig {
  /** OpenID Connect issuer, e.g. http://localhost:8180/realms/onboarding */
  issuer: string;
  clientId: string;
  /** Only false for local development over plain HTTP. */
  requireHttps?: boolean;
}

export const RUNTIME_CONFIG = new InjectionToken<RuntimeConfig>('RUNTIME_CONFIG');
