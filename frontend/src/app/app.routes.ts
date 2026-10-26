import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { roleGuard } from './core/role.guard';

export const routes: Routes = [
  { path: '', component: HomeComponent, title: 'Onboarding Portal' },
  {
    path: 'apply',
    canActivate: [roleGuard('applicant')],
    loadComponent: () => import('./applicant/apply.component').then((m) => m.ApplyComponent),
    title: 'New application',
  },
  {
    path: 'my-applications',
    canActivate: [roleGuard('applicant')],
    loadComponent: () =>
      import('./applicant/my-applications.component').then((m) => m.MyApplicationsComponent),
    title: 'My applications',
  },
  {
    path: 'review',
    canActivate: [roleGuard('caseworker')],
    loadComponent: () => import('./review/review-list.component').then((m) => m.ReviewListComponent),
    title: 'Review queue',
  },
  {
    path: 'review/:id',
    canActivate: [roleGuard('caseworker')],
    loadComponent: () => import('./review/review-detail.component').then((m) => m.ReviewDetailComponent),
    title: 'Review application',
  },
  { path: '**', redirectTo: '' },
];
