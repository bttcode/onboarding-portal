import { HttpErrorResponse } from '@angular/common/http';
import { Problem } from '../api/models';

/** Turns an API error into one sentence for the user, using the RFC 9457 `detail` when present. */
export function problemMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const problem = parseProblem(error.error);
    if (problem?.detail) {
      return problem.detail;
    }
    switch (error.status) {
      case 0:
        return 'The server cannot be reached. Check your connection and try again.';
      case 401:
        return 'Your session has expired. Please log in again.';
      case 403:
        return 'You are not allowed to do this.';
      case 404:
        return 'This application was not found.';
      case 413:
        return 'The file is too large.';
      default:
        return `Something went wrong (HTTP ${error.status}).`;
    }
  }
  return 'Something went wrong.';
}

function parseProblem(body: unknown): Problem | null {
  if (typeof body === 'string') {
    try {
      return JSON.parse(body) as Problem;
    } catch {
      return null;
    }
  }
  return body && typeof body === 'object' ? (body as Problem) : null;
}
