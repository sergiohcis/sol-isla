import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CurrentUser } from './current-user.model';
import { CurrentUserService } from './current-user.service';

describe('CurrentUserService', () => {
  let service: CurrentUserService;
  let httpMock: HttpTestingController;

  const user: CurrentUser = { id: '1', email: 'admin@a.test', role: 'ADMIN' };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CurrentUserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('sets the current user on successful login', () => {
    service.login('admin@a.test', 'secret').subscribe();

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush(user);

    expect(service.currentUser()).toEqual(user);
    expect(service.isAuthenticated()).toBe(true);
  });

  it('restoreSession clears state on a 401 instead of throwing', () => {
    let result: CurrentUser | null | undefined;
    service.restoreSession().subscribe((value) => (result = value));

    httpMock.expectOne('/api/auth/me').flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(result).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
  });

  it('logout clears the current user', () => {
    service.login('admin@a.test', 'secret').subscribe();
    httpMock.expectOne('/api/auth/login').flush(user);
    expect(service.isAuthenticated()).toBe(true);

    service.logout().subscribe();
    httpMock.expectOne('/api/auth/logout').flush(null);

    expect(service.isAuthenticated()).toBe(false);
  });

  it('clearSession drops local state without calling the backend', () => {
    service.login('admin@a.test', 'secret').subscribe();
    httpMock.expectOne('/api/auth/login').flush(user);

    service.clearSession();

    expect(service.isAuthenticated()).toBe(false);
  });

  it('isAdmin is false for a STAFF role and true for an ADMIN role', () => {
    service.login('staff@a.test', 'secret').subscribe();
    httpMock.expectOne('/api/auth/login').flush({ ...user, role: 'STAFF' });
    expect(service.isAdmin()).toBe(false);

    service.login('admin@a.test', 'secret').subscribe();
    httpMock.expectOne('/api/auth/login').flush({ ...user, role: 'ADMIN' });
    expect(service.isAdmin()).toBe(true);
  });
});
