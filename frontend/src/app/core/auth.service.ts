import { Injectable, computed, inject, signal } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { AccessTokenClaims, decodeJwtPayload } from './jwt';
import { RUNTIME_CONFIG } from './runtime-config';

export type Role = 'applicant' | 'caseworker';

/**
 * Login with Keycloak using the authorization-code flow with PKCE. The SPA is a public client:
 * it holds no secret, and tokens are refreshed with the refresh token instead of a hidden iframe.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly oauth = inject(OAuthService);
  private readonly config = inject(RUNTIME_CONFIG);

  private readonly claims = signal<AccessTokenClaims | null>(null);
  readonly available = signal(true);

  readonly loggedIn = computed(() => this.claims() !== null);
  readonly username = computed(() => this.claims()?.preferred_username ?? '');
  readonly displayName = computed(() => this.claims()?.name ?? this.username());
  readonly roles = computed<string[]>(() => this.claims()?.realm_access?.roles ?? []);
  readonly isApplicant = computed(() => this.roles().includes('applicant'));
  readonly isCaseworker = computed(() => this.roles().includes('caseworker'));

  async init(): Promise<void> {
    this.oauth.configure({
      issuer: this.config.issuer,
      clientId: this.config.clientId,
      redirectUri: window.location.origin + '/',
      postLogoutRedirectUri: window.location.origin + '/',
      responseType: 'code',
      scope: 'openid profile email',
      requireHttps: this.config.requireHttps ?? true,
      useSilentRefresh: false,
      timeoutFactor: 0.75,
      showDebugInformation: false,
    });
    this.oauth.events.subscribe(() => this.refreshClaims());
    try {
      await this.oauth.loadDiscoveryDocumentAndTryLogin();
      this.oauth.setupAutomaticSilentRefresh();
    } catch (error) {
      // keep the app usable (and say why) when the identity provider is down
      console.error('Login service unavailable', error);
      this.available.set(false);
    }
    this.refreshClaims();
  }

  login(): void {
    this.oauth.initCodeFlow();
  }

  logout(): void {
    this.oauth.logOut();
  }

  hasRole(role: Role): boolean {
    return this.roles().includes(role);
  }

  private refreshClaims(): void {
    const token = this.oauth.hasValidAccessToken() ? this.oauth.getAccessToken() : null;
    this.claims.set(token ? decodeJwtPayload<AccessTokenClaims>(token) : null);
  }
}
