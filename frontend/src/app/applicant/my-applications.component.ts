import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApplicationSummary } from '../api/models';
import { ApplicationsService } from '../api/services';
import { PRODUCT_LABELS } from '../core/labels';
import { problemMessage } from '../core/problem';
import { StatusChipComponent } from '../shared/status-chip.component';

@Component({
  selector: 'app-my-applications',
  imports: [DatePipe, RouterLink, MatButtonModule, MatCardModule, MatProgressBarModule, StatusChipComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="page">
      <div class="header">
        <div>
          <h1 class="page-title">My applications</h1>
          <p class="page-subtitle">Follow the review of your applications here.</p>
        </div>
        <a mat-flat-button routerLink="/apply">New application</a>
      </div>

      @if (loading()) {
        <mat-progress-bar mode="indeterminate" />
      }
      @if (error(); as message) {
        <p class="error-banner" role="alert">{{ message }}</p>
      }

      <div class="cards">
        @for (app of applications(); track app.id) {
          <mat-card>
            <mat-card-header>
              <mat-card-title>{{ productLabels[app.product] }}</mat-card-title>
              <mat-card-subtitle>Submitted {{ app.submittedAt | date: 'mediumDate' }}</mat-card-subtitle>
            </mat-card-header>
            <mat-card-content>
              <div class="chips">
                <app-status-chip [status]="app.status" />
                <app-status-chip [status]="app.checkStatus" />
              </div>
              <p class="muted">
                {{ app.hasDocument ? 'Document uploaded' : 'No document uploaded yet' }}
              </p>
            </mat-card-content>
          </mat-card>
        } @empty {
          @if (!loading() && !error()) {
            <p class="muted">You have no applications yet.</p>
          }
        }
      </div>
    </section>
  `,
  styles: `
    .header {
      display: flex;
      flex-wrap: wrap;
      gap: 16px;
      justify-content: space-between;
      align-items: flex-start;
    }
    .cards {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
      gap: 16px;
      margin-top: 16px;
    }
    .chips {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      margin: 8px 0;
    }
  `,
})
export class MyApplicationsComponent implements OnInit {
  private readonly api = inject(ApplicationsService);

  protected readonly productLabels = PRODUCT_LABELS;
  protected readonly applications = signal<ApplicationSummary[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.api
      .listMyApplications()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (applications) => this.applications.set(applications),
        error: (err: unknown) => this.error.set(problemMessage(err)),
      });
  }
}
