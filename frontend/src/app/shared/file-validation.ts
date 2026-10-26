import { ValidationErrors } from '@angular/forms';

export const ACCEPTED_TYPES = ['application/pdf', 'image/png', 'image/jpeg'];
export const MAX_FILE_BYTES = 5 * 1024 * 1024;

/**
 * Client-side check for a quick answer. It trusts the browser's file type, so the backend checks
 * the real file signature again.
 */
export function validateFile(
  file: File | null,
  accept: readonly string[] = ACCEPTED_TYPES,
  maxBytes: number = MAX_FILE_BYTES,
): ValidationErrors | null {
  if (!file) {
    return null;
  }
  if (!accept.includes(file.type)) {
    return { fileType: { actual: file.type || 'unknown' } };
  }
  if (file.size > maxBytes) {
    return { fileSize: { max: maxBytes, actual: file.size } };
  }
  return null;
}

export function formatBytes(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(0)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
