import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatToolbarModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <mat-toolbar class="toolbar">
      <a routerLink="/" class="brand">Onboarding Portal</a>
      <nav class="nav">
        @if (auth.isApplicant()) {
          <a mat-button routerLink="/my-applications" routerLinkActive="active">My applications</a>
          <a mat-button routerLink="/apply" routerLinkActive="active">Apply</a>
        }
        @if (auth.isCaseworker()) {
          <a mat-button routerLink="/review" routerLinkActive="active">Review queue</a>
        }
      </nav>
      <span class="spacer"></span>
      @if (auth.loggedIn()) {
        <span class="user">{{ auth.displayName() }}</span>
        <button mat-stroked-button type="button" (click)="auth.logout()">Log out</button>
      } @else {
        <button mat-flat-button type="button" (click)="auth.login()" [disabled]="!auth.available()">
          Log in
        </button>
      }
    </mat-toolbar>
    @if (!auth.available()) {
      <div class="page"><p class="error-banner">The login service is unavailable. Try again in a minute.</p></div>
    }
    <main>
      <router-outlet />
    </main>
  `,
  styles: `
    .toolbar {
      position: sticky;
      top: 0;
      z-index: 10;
      flex-wrap: wrap;
      gap: 8px;
      height: auto;
      min-height: 64px;
      background: #fff;
      border-bottom: 1px solid #e0e3eb;
    }
    .brand {
      color: inherit;
      text-decoration: none;
      font-weight: 500;
      margin-right: 16px;
    }
    .nav {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
    }
    .nav .active {
      background: #e8f0fe;
    }
    .spacer {
      flex: 1;
    }
    .user {
      font-size: 0.9rem;
      color: #5f6368;
      margin-right: 8px;
    }
    @media (max-width: 640px) {
      .user {
        display: none;
      }
    }
  `,
})
export class AppComponent {
  protected readonly auth = inject(AuthService);
}
