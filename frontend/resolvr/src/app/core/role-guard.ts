import {CanActivateFn, Router} from '@angular/router';
import {inject} from '@angular/core';
import {Auth} from './auth';

export const roleGuard = (roles: string[]): CanActivateFn => () => {
  const auth = inject(Auth);
  const router = inject(Router);
  if (auth.hasRole(roles)) return true;
  return router.createUrlTree(['/app/dashboard']);
};
