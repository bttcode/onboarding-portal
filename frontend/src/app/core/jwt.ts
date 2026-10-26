/** The access-token claims the UI uses. The backend re-checks everything; this only drives the UI. */
export interface AccessTokenClaims {
  sub: string;
  preferred_username?: string;
  name?: string;
  realm_access?: { roles?: string[] };
}

/** Decodes the payload of a JWT without verifying it (verification is the backend's job). */
export function decodeJwtPayload<T>(token: string): T | null {
  const payload = token.split('.')[1];
  if (!payload) {
    return null;
  }
  try {
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
    const bytes = Uint8Array.from(atob(padded), (char) => char.charCodeAt(0));
    return JSON.parse(new TextDecoder().decode(bytes)) as T;
  } catch {
    return null;
  }
}
