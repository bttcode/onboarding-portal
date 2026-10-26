import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';

/** Landing page. Logged-in users go straight to the page for their role. */
@Component({
  selector: 'app-home',
  imports: [MatButtonModule, MatCardModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="page">
      <mat-card class="hero">
        <mat-card-content>
          <h1 class="page-title">Apply for insurance online</h1>
          <p class="page-subtitle">
            Fill in one form, upload your ID, and follow the review of your application.
          </p>
          @if (!auth.loggedIn()) {
            <button mat-flat-button type="button" (click)="auth.login()" [disabled]="!auth.available()">
              Log in or register
            </button>
          } @else {
            <p class="muted">Your account has no portal role yet. Ask an administrator for access.</p>
          }
        </mat-card-content>
      </mat-card>
    </section>
  `,
  styles: `
    .hero {
      max-width: 640px;
      margin: 48px auto 0;
      padding: 16px;
    }
  `,
})
export class HomeComponent implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    if (this.auth.isCaseworker()) {
      void this.router.navigate(['/review']);
    } else if (this.auth.isApplicant()) {
      void this.router.navigate(['/my-applications']);
    }
  }
}
