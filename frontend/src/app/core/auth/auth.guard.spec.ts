import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { authGuard } from './auth.guard';
import { CurrentUserService } from './current-user.service';

describe('authGuard', () => {
  let currentUserService: CurrentUserService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    currentUserService = TestBed.inject(CurrentUserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  function runGuard() {
    return TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url: '/admin/products' } as RouterStateSnapshot),
    );
  }

  it('allows navigation when authenticated', () => {
    currentUserService.login('a@test.com', 'secret').subscribe();
    httpMock.expectOne('/api/auth/login').flush({ id: '1', email: 'a@test.com', role: 'ADMIN' });

    expect(runGuard()).toBe(true);
  });

  it('redirects to /admin/login with a returnUrl when not authenticated', () => {
    const router = TestBed.inject(Router);
    const result = runGuard() as UrlTree;

    expect(result).toBeInstanceOf(UrlTree);
    expect(router.serializeUrl(result)).toBe('/admin/login?returnUrl=%2Fadmin%2Fproducts');
  });
});
