import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { Observable, catchError, finalize, of, switchMap, tap } from 'rxjs';
import { ApplicationDetail, DecisionType } from '../api/models';
import { ApplicationsService } from '../api/services';
import { AuthService } from '../core/auth.service';
import { NATIONALITIES, PRODUCT_LABELS } from '../core/labels';
import { problemMessage } from '../core/problem';
import { StatusChipComponent } from '../shared/status-chip.component';

@Component({
  selector: 'app-review-detail',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
    StatusChipComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './review-detail.component.html',
  styles: `
    .layout {
      display: grid;
      grid-template-columns: 2fr 1fr;
      gap: 16px;
      align-items: start;
    }
    .details {
      display: grid;
      grid-template-columns: max-content 1fr;
      gap: 6px 16px;
      margin: 0;
    }
    .details dt {
      color: #5f6368;
    }
    .details dd {
      margin: 0;
      word-break: break-word;
    }
    .side {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
    .decision {
      padding: 8px 0;
      border-bottom: 1px solid #eceff1;
    }
    .decision:last-child {
      border-bottom: none;
    }
    .warning {
      padding: 8px 12px;
      border-radius: 8px;
      background: #fff4e0;
      color: #8a4b00;
    }
    .comment {
      width: 100%;
    }
    @media (max-width: 860px) {
      .layout {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class ReviewDetailComponent {
  private readonly api = inject(ApplicationsService);
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  /** Bound from the route parameter by withComponentInputBinding(). */
  readonly id = input.required<string>();

  protected readonly productLabels = PRODUCT_LABELS;
  protected readonly application = signal<ApplicationDetail | null>(null);
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly comment = new FormControl('', { nonNullable: true, validators: Validators.maxLength(1000) });

  protected readonly nationality = computed(() => {
    const code = this.application()?.applicant.nationality;
    return NATIONALITIES.find((country) => country.code === code)?.name ?? code;
  });

  protected readonly alreadyDecidedByMe = computed(
    () => this.application()?.decisions.some((d) => d.caseworker === this.auth.username()) ?? false,
  );

  protected readonly canDecide = computed(
    () => this.application()?.status === 'IN_REVIEW' && !this.alreadyDecidedByMe(),
  );

  protected readonly approvals = computed(
    () => this.application()?.decisions.filter((d) => d.decision === 'APPROVE').length ?? 0,
  );

  constructor() {
    toObservable(this.id)
      .pipe(
        tap(() => {
          this.loading.set(true);
          this.error.set(null);
        }),
        switchMap((id) =>
          this.api.getApplication({ id }).pipe(
            catchError((err: unknown) => {
              this.error.set(problemMessage(err));
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((application) => {
        this.loading.set(false);
        this.application.set(application);
      });
  }

  decide(decision: DecisionType): void {
    const comment = this.comment.value.trim() || undefined;
    this.run(this.api.decideApplication({ id: this.id(), body: { decision, comment } }), () => {
      this.comment.reset();
      this.snackBar.open(decision === 'APPROVE' ? 'Approval recorded.' : 'Application rejected.', 'OK', {
        duration: 4000,
      });
    });
  }

  retryCheck(): void {
    this.run(this.api.retryCheck({ id: this.id() }), () =>
      this.snackBar.open('The background check is queued again.', 'OK', { duration: 4000 }),
    );
  }

  downloadDocument(): void {
    const name = this.application()?.documentName ?? 'document';
    this.busy.set(true);
    this.api
      .downloadDocument({ id: this.id() })
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = name;
          link.click();
          URL.revokeObjectURL(url);
        },
        error: (err: unknown) => this.error.set(problemMessage(err)),
      });
  }

  private run(call: Observable<ApplicationDetail>, onSuccess: () => void): void {
    this.busy.set(true);
    this.error.set(null);
    call.pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (updated) => {
        this.application.set(updated);
        onSuccess();
      },
      error: (err: unknown) => this.error.set(problemMessage(err)),
    });
  }
}
