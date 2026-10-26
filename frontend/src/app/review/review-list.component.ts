import { BreakpointObserver } from '@angular/cdk/layout';
import { DatePipe } from '@angular/common';
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { Router } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, map, merge, of, startWith, switchMap, tap } from 'rxjs';
import { ApplicationPage, ApplicationStatus, ApplicationSummary } from '../api/models';
import { ApplicationsService } from '../api/services';
import { PRODUCT_LABELS } from '../core/labels';
import { problemMessage } from '../core/problem';
import { StatusChipComponent } from '../shared/status-chip.component';

const ALL_COLUMNS = ['lastName', 'product', 'submittedAt', 'status', 'check', 'approvals'];
const COMPACT_COLUMNS = ['lastName', 'status', 'check'];

/**
 * The caseworkers' queue. Filtering, sorting and paging all happen on the server, so the page
 * stays fast however many applications exist. Oldest first by default: first in, first reviewed.
 */
@Component({
  selector: 'app-review-list',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    MatSortModule,
    MatTableModule,
    StatusChipComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './review-list.component.html',
  styles: `
    .filters {
      display: flex;
      flex-wrap: wrap;
      gap: 16px;
    }
    .filters mat-form-field {
      flex: 1 1 220px;
    }
    .table-wrapper {
      overflow-x: auto;
      background: #fff;
      border-radius: 12px;
      border: 1px solid #e0e3eb;
    }
    table {
      width: 100%;
    }
    tr.row {
      cursor: pointer;
    }
    tr.row:hover,
    tr.row:focus-visible {
      background: #f1f4fb;
      outline: none;
    }
    .empty {
      padding: 24px;
      text-align: center;
    }
  `,
})
export class ReviewListComponent implements AfterViewInit {
  private readonly api = inject(ApplicationsService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  private readonly paginator = viewChild.required(MatPaginator);
  private readonly sort = viewChild.required(MatSort);

  protected readonly statuses: { value: ApplicationStatus | ''; label: string }[] = [
    { value: '', label: 'All' },
    { value: 'SUBMITTED', label: 'Submitted (check running)' },
    { value: 'IN_REVIEW', label: 'In review' },
    { value: 'APPROVED', label: 'Approved' },
    { value: 'REJECTED', label: 'Rejected' },
  ];

  protected readonly statusFilter = new FormControl<ApplicationStatus | ''>('IN_REVIEW', { nonNullable: true });
  protected readonly search = new FormControl('', { nonNullable: true });

  protected readonly rows = signal<ApplicationSummary[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  private readonly compact = toSignal(
    inject(BreakpointObserver).observe('(max-width: 720px)').pipe(map((state) => state.matches)),
    { initialValue: false },
  );
  protected readonly columns = computed(() => (this.compact() ? COMPACT_COLUMNS : ALL_COLUMNS));

  ngAfterViewInit(): void {
    const paginator = this.paginator();
    const sort = this.sort();

    const filters$ = merge(
      this.statusFilter.valueChanges,
      this.search.valueChanges.pipe(debounceTime(300), distinctUntilChanged()),
    );
    // a new filter or sort order starts again at the first page
    merge(filters$, sort.sortChange)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => (paginator.pageIndex = 0));

    merge(filters$, sort.sortChange, paginator.page)
      .pipe(
        startWith(null),
        tap(() => {
          this.loading.set(true);
          this.error.set(null);
        }),
        // switchMap drops the response of an outdated query when the user types quickly
        switchMap(() =>
          this.api
            .searchApplications({
              status: this.statusFilter.value || undefined,
              q: this.search.value.trim() || undefined,
              page: paginator.pageIndex,
              size: paginator.pageSize,
              sort: `${sort.active || 'submittedAt'},${sort.direction || 'asc'}`,
            })
            .pipe(
              catchError((err: unknown) => {
                this.error.set(problemMessage(err));
                return of<ApplicationPage | null>(null);
              }),
            ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((page) => {
        this.loading.set(false);
        if (page) {
          this.rows.set(page.content);
          this.total.set(page.totalElements);
        }
      });
  }

  protected productLabel(row: ApplicationSummary): string {
    return PRODUCT_LABELS[row.product];
  }

  open(row: ApplicationSummary): void {
    void this.router.navigate(['/review', row.id]);
  }
}
