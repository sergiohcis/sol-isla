import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { CurrentUserService } from './current-user.service';

const AUTH_ENDPOINTS_WITH_EXPECTED_401 = ['/api/auth/login', '/api/auth/me'];

/**
 * A 401 from /api/auth/login (bad credentials) or /api/auth/me (not logged in yet) is normal,
 * expected control flow, not a session that "expired mid-use" — only redirect for the latter.
 */
export const sessionExpiredInterceptor: HttpInterceptorFn = (req, next) => {
  const currentUserService = inject(CurrentUserService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: unknown) => {
      const isExpectedEndpoint = AUTH_ENDPOINTS_WITH_EXPECTED_401.some((endpoint) =>
        req.url.endsWith(endpoint),
      );
      if (error instanceof HttpErrorResponse && error.status === 401 && !isExpectedEndpoint) {
        currentUserService.clearSession();
        router.navigate(['/admin/login']);
      }
      return throwError(() => error);
    }),
  );
};
