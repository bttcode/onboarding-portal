import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, Role } from './auth.service';

/** Sends anonymous users to the login page and users without the role back home. */
export function roleGuard(role: Role): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    if (!auth.loggedIn()) {
      auth.login();
      return false;
    }
    return auth.hasRole(role) ? true : inject(Router).createUrlTree(['/']);
  };
}
