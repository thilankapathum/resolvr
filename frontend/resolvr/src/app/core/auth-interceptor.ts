import {HttpErrorResponse, HttpHandlerFn, HttpInterceptorFn, HttpRequest} from '@angular/common/http';
import {inject} from '@angular/core';
import {Auth} from './auth';
import {BehaviorSubject, catchError, filter, Observable, switchMap, take, throwError} from 'rxjs';

// Shared state to prevent multiple simultaneous refresh calls
let isRefreshing = false;
let refreshSubject = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) =>
{
  const authService = inject(Auth);

  // Skip auth header for auth endpoints themselves
  if (isAuthEndpoint(req.url)) {
    return next(req);
  }

  const token = authService.getAccessToken();
  const authReq = token ? addToken(req, token) : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        return handle401(req, next, authService);
      }
      return throwError(() => error);
    })
  );
};

function handle401(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: Auth
): Observable<any> {
  if (!isRefreshing) {
    isRefreshing = true;
    refreshSubject.next(null);

    return authService.refreshAccessToken().pipe(
      switchMap((res) => {
        isRefreshing = false;
        const newToken = authService.getAccessToken()!;
        refreshSubject.next(newToken);
        return next(addToken(req, newToken));
      }),
      catchError((err) => {
        isRefreshing = false;
        refreshSubject.next(null);
        // Refresh failed — session is truly expired, force logout
        authService.logout();
        return throwError(() => err);
      })
    );
  } else {
    // Another request is already refreshing — wait for the new token
    return refreshSubject.pipe(
      filter(token => token !== null),
      take(1),
      switchMap(token => next(addToken(req, token!)))
    );
  }
}

function addToken(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

function isAuthEndpoint(url: string): boolean {
  return url.includes('/auth/login')
    || url.includes('/auth/register')
    || url.includes('/auth/refresh-token')
    || url.includes('/auth/forgot-password')
    || url.includes('/auth/reset-password');
}
