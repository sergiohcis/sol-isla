import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { CurrentUserService } from './current-user.service';

/**
 * UX-only: hides admin pages from a signed-out visitor. The backend is the sole authority on
 * access — this guard never substitutes for a server-side @PreAuthorize check (CLAUDE.md rule 9).
 * Only guards the admin surface; the public storefront (catalog/cart/checkout) has no guard at
 * all — guest checkout is the MVP default (rule 11).
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const currentUserService = inject(CurrentUserService);
  const router = inject(Router);

  if (currentUserService.isAuthenticated()) {
    return true;
  }
  return router.createUrlTree(['/admin/login'], { queryParams: { returnUrl: state.url } });
};
