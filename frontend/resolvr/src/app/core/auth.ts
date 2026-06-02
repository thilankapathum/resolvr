import {computed, inject, Injectable, OnDestroy, signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Router} from '@angular/router';
import {AuthResponse, UserResponse} from './models/models';
import {catchError, EMPTY, tap} from 'rxjs';
import {environment} from '../../../environments/environment'

const ACCESS_TOKEN_KEY  = 'resolvr_access';
const REFRESH_TOKEN_KEY = 'resolvr_refresh';
const USER_KEY          = 'resolvr_user';

@Injectable({ providedIn: 'root' })
export class Auth implements OnDestroy{
  private readonly http   = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly api    = `${environment.apiUrl}/auth`;

  private readonly _currentUser = signal<UserResponse | null>(this.loadUser());
  readonly currentUser = this._currentUser.asReadonly();

  readonly isLoggedIn = computed(() => !!this._currentUser());
  readonly isAdmin    = computed(() => this._currentUser()?.role === 'ADMIN');
  readonly isManager  = computed(() => this._currentUser()?.role === 'MANAGER');
  readonly isHead     = computed(() => this._currentUser()?.role === 'HEAD');
  readonly isEngineer = computed(() => this._currentUser()?.role === 'ENGINEER');
  readonly isTO       = computed(() => this._currentUser()?.role === 'TECHNICAL_OFFICER');
  readonly userRole   = computed(() => this._currentUser()?.role ?? null);

  private refreshTimerHandle: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    // On app startup, if already logged in, start the refresh timer
    if (this.isLoggedIn()) {
      this.scheduleProactiveRefresh();
    }
  }

  hasRole(roles: string[]): boolean {
    const role = this._currentUser()?.role;
    return !!role && roles.includes(role);
  }

  login(email: string, password: string) {
    return this.http.post<AuthResponse>(`${this.api}/login`, { email, password }).pipe(
      tap(res => {
        this.storeSession(res);
        this.scheduleProactiveRefresh();
      })
    );
  }

  register(fullName: string, email: string, password: string) {
    return this.http.post<{ message: string }>(`${this.api}/register`, { fullName, email, password });
  }

  verifyEmail(token: string) {
    return this.http.get<{ message: string }>(`${this.api}/verify-email`, { params: { token } });
  }

  forgotPassword(email: string) {
    return this.http.post<{ message: string }>(`${this.api}/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string) {
    return this.http.post<{ message: string }>(`${this.api}/reset-password`, { token, newPassword });
  }

  changePassword(currentPassword: string, newPassword: string) {
    return this.http.post<{ message: string }>(`${this.api}/change-password`, { currentPassword, newPassword });
  }

  refreshAccessToken() {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) return EMPTY;
    return this.http.post<AuthResponse>(`${this.api}/refresh-token`, { refreshToken }).pipe(
      tap(res => {
        this.storeSession(res);
        this.scheduleProactiveRefresh();
      }),
      catchError(() => {
        this.logout();
        return EMPTY;
      })
    );
  }

  logout() {
    const refreshToken = this.getRefreshToken();
    if (refreshToken) {
      this.http.post(`${this.api}/logout`, { refreshToken }).subscribe();
    }
    this.clearRefreshTimer();
    this.clearSession();
    this.router.navigate(['/login']);
  }

  getAccessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  getCurrentUserProfile() {
    return this.http.get<UserResponse>(`${this.api}/me`).pipe(
      tap(user => {
        localStorage.setItem(USER_KEY, JSON.stringify(user));
        this._currentUser.set(user);
      })
    );
  }

  // ── Token refresh timer ───────────────────────────────────

  private scheduleProactiveRefresh() {
    this.clearRefreshTimer();

    const token = this.getAccessToken();
    if (!token) return;

    const expiryMs = this.getTokenExpiryMs(token);
    if (expiryMs === null) return;

    // Refresh 2 minutes before expiry
    const refreshInMs = expiryMs - Date.now() - (2 * 60 * 1000);

    if (refreshInMs <= 0) {
      // Token already expired or expiring very soon — refresh immediately
      this.refreshAccessToken().subscribe();
      return;
    }

    this.refreshTimerHandle = setTimeout(() => {
      this.refreshAccessToken().subscribe();
    }, refreshInMs);
  }

  private clearRefreshTimer() {
    if (this.refreshTimerHandle !== null) {
      clearTimeout(this.refreshTimerHandle);
      this.refreshTimerHandle = null;
    }
  }

  private getTokenExpiryMs(token: string): number | null {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.exp ? payload.exp * 1000 : null;
    } catch {
      return null;
    }
  }

  // ── Session storage ───────────────────────────────────────

  private storeSession(res: AuthResponse): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, res.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, res.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(res.user));
    this._currentUser.set(res.user);
  }

  private clearSession(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this._currentUser.set(null);
  }

  private loadUser(): UserResponse | null {
    try {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }

  ngOnDestroy() {
    this.clearRefreshTimer();
  }
}
