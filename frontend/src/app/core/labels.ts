import { ApplicationStatus, CheckStatus, Product } from '../api/models';

export const PRODUCT_LABELS: Record<Product, string> = {
  LIFE: 'Life insurance',
  HOUSEHOLD: 'Household insurance',
  LIABILITY: 'Personal liability',
};

export const NATIONALITIES: ReadonlyArray<{ code: string; name: string }> = [
  { code: 'CH', name: 'Switzerland' },
  { code: 'AT', name: 'Austria' },
  { code: 'DE', name: 'Germany' },
  { code: 'FR', name: 'France' },
  { code: 'IT', name: 'Italy' },
  { code: 'LI', name: 'Liechtenstein' },
  { code: 'PT', name: 'Portugal' },
  { code: 'ES', name: 'Spain' },
  { code: 'GB', name: 'United Kingdom' },
  { code: 'US', name: 'United States' },
  { code: 'VN', name: 'Vietnam' },
];

export type AnyStatus = ApplicationStatus | CheckStatus;

export type Tone = 'neutral' | 'info' | 'success' | 'warning' | 'danger';

export const STATUS_PRESENTATION: Record<AnyStatus, { label: string; tone: Tone }> = {
  SUBMITTED: { label: 'Submitted', tone: 'neutral' },
  IN_REVIEW: { label: 'In review', tone: 'info' },
  APPROVED: { label: 'Approved', tone: 'success' },
  REJECTED: { label: 'Rejected', tone: 'danger' },
  PENDING: { label: 'Check pending', tone: 'neutral' },
  CLEAR: { label: 'Check clear', tone: 'success' },
  HIT: { label: 'Check hit', tone: 'warning' },
  ERROR: { label: 'Check failed', tone: 'danger' },
};
