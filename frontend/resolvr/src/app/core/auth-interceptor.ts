import {HttpErrorResponse, HttpInterceptorFn} from '@angular/common/http';
import {inject} from '@angular/core';
import {Auth} from './auth';
import {catchError, switchMap, throwError} from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) =>
{
  const authService = inject(Auth);
  const token = authService.getAccessToken();

  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // On 401, attempt token refresh once
      if (error.status === 401 && !req.url.includes('/auth/')) {
        return authService.refreshAccessToken().pipe(
          switchMap(() => {
            const newToken = authService.getAccessToken();
            const retried = req.clone({
              setHeaders: { Authorization: `Bearer ${newToken}` }
            });
            return next(retried);
          }),
          catchError(refreshError => {
            authService.logout();
            return throwError(() => refreshError);
          })
        );
      }
      return throwError(() => error);
    })
  );
};
