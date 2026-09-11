import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, of, tap } from 'rxjs';
import { CurrentUser } from './current-user.model';

@Injectable({ providedIn: 'root' })
export class CurrentUserService {
  private readonly http = inject(HttpClient);

  private readonly currentUserSignal = signal<CurrentUser | null>(null);
  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUserSignal() !== null);
  readonly isAdmin = computed(() => this.currentUserSignal()?.role === 'ADMIN');

  login(email: string, password: string): Observable<CurrentUser> {
    return this.http
      .post<CurrentUser>('/api/auth/login', { email, password })
      .pipe(tap((user) => this.currentUserSignal.set(user)));
  }

  logout(): Observable<void> {
    return this.http
      .post<void>('/api/auth/logout', {})
      .pipe(tap(() => this.currentUserSignal.set(null)));
  }

  /** Called once at app bootstrap (see provideAppInitializer in app.config.ts). A 401 here just
   *  means "not logged in yet" — not an error worth surfacing. */
  restoreSession(): Observable<CurrentUser | null> {
    return this.http.get<CurrentUser>('/api/auth/me').pipe(
      tap((user) => this.currentUserSignal.set(user)),
      catchError(() => {
        this.currentUserSignal.set(null);
        return of(null);
      }),
    );
  }

  /** Clears local session state without calling the backend — used by the session-expired
   *  interceptor when a previously-valid session stops being accepted mid-use. */
  clearSession(): void {
    this.currentUserSignal.set(null);
  }
}
