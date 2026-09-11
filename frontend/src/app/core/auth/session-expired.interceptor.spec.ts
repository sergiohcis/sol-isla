import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { CurrentUserService } from './current-user.service';
import { sessionExpiredInterceptor } from './session-expired.interceptor';

describe('sessionExpiredInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let currentUserService: CurrentUserService;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([sessionExpiredInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    currentUserService = TestBed.inject(CurrentUserService);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('clears the session and redirects to /admin/login on a 401 from a protected endpoint', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');

    http.get('/api/admin/orders').subscribe({ error: () => {} });
    httpMock.expectOne('/api/admin/orders').flush('Forbidden', { status: 401, statusText: 'Unauthorized' });

    expect(navigateSpy).toHaveBeenCalledWith(['/admin/login']);
  });

  it('does not redirect on a 401 from /api/auth/login', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');

    http.post('/api/auth/login', {}).subscribe({ error: () => {} });
    httpMock.expectOne('/api/auth/login').flush('Invalid credentials', { status: 401, statusText: 'Unauthorized' });

    expect(navigateSpy).not.toHaveBeenCalled();
  });

  it('does not redirect on a 401 from /api/auth/me', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');

    http.get('/api/auth/me').subscribe({ error: () => {} });
    httpMock.expectOne('/api/auth/me').flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(navigateSpy).not.toHaveBeenCalled();
  });
});
