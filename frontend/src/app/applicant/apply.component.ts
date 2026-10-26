import { BreakpointObserver } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatStepperModule } from '@angular/material/stepper';
import { Router } from '@angular/router';
import { finalize, map, of, switchMap } from 'rxjs';
import { ApplicantData, Product } from '../api/models';
import { ApplicationsService } from '../api/services';
import { NATIONALITIES, PRODUCT_LABELS } from '../core/labels';
import { problemMessage } from '../core/problem';
import { FileDropComponent } from '../shared/file-drop.component';
import { minimumAge, notInPast, todayIso } from './date-validators';

/** Three-step application form: personal data, address and product, document and consent. */
@Component({
  selector: 'app-apply',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule,
    MatSelectModule,
    MatStepperModule,
    FileDropComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './apply.component.html',
  styles: `
    .products {
      display: flex;
      flex-direction: column;
      gap: 4px;
      margin-bottom: 16px;
    }
    .summary {
      display: grid;
      grid-template-columns: max-content 1fr;
      gap: 4px 16px;
      margin: 16px 0;
    }
    .summary dt {
      color: #5f6368;
    }
    .summary dd {
      margin: 0;
    }
  `,
})
export class ApplyComponent {
  private readonly api = inject(ApplicationsService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly nationalities = NATIONALITIES;
  protected readonly products = Object.entries(PRODUCT_LABELS) as [Product, string][];
  protected readonly productLabels = PRODUCT_LABELS;
  protected readonly today = todayIso();

  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);

  /** Vertical steps on phones, horizontal on wider screens. */
  protected readonly vertical = toSignal(
    inject(BreakpointObserver).observe('(max-width: 720px)').pipe(map((state) => state.matches)),
    { initialValue: false },
  );

  protected readonly personal = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    birthDate: ['', [Validators.required, minimumAge(18)]],
    nationality: ['CH', Validators.required],
  });

  protected readonly address = this.fb.group({
    street: ['', [Validators.required, Validators.maxLength(200)]],
    postalCode: ['', [Validators.required, Validators.pattern(/^[1-9]\d{3}$/)]],
    city: ['', [Validators.required, Validators.maxLength(100)]],
    product: ['HOUSEHOLD' as Product, Validators.required],
    startDate: [this.today, [Validators.required, notInPast()]],
  });

  protected readonly confirmation = this.fb.group({
    document: this.fb.control<File | null>(null),
    consent: [false, Validators.requiredTrue],
  });

  submit(): void {
    if (this.personal.invalid || this.address.invalid || this.confirmation.invalid || this.submitting()) {
      return;
    }
    const body: ApplicantData = { ...this.personal.getRawValue(), ...this.address.getRawValue() };
    const file = this.confirmation.controls.document.value;

    this.submitting.set(true);
    this.error.set(null);
    this.api
      .createApplication({ body })
      .pipe(
        switchMap((created) =>
          file
            ? this.api.uploadDocument({ id: created.id, body: { file } }).pipe(map(() => created))
            : of(created),
        ),
        finalize(() => this.submitting.set(false)),
      )
      .subscribe({
        next: () => {
          this.snackBar.open('Application submitted. We will run the background check now.', 'OK', {
            duration: 5000,
          });
          void this.router.navigate(['/my-applications']);
        },
        error: (err: unknown) => this.error.set(problemMessage(err)),
      });
  }
}
