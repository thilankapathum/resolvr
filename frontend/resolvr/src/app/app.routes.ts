import { Routes } from '@angular/router';
import {noAuthGuard} from './core/no-auth-guard';
import {authGuard} from './core/auth-guard';
import {roleGuard} from './core/role-guard';

export const routes: Routes = [
  // Auth layout routes (no sidebar)
  {
    path: '',
    loadComponent: () => import('./layouts/auth-layout-component/auth-layout-component')
      .then(m => m.AuthLayoutComponent),
    canActivate: [noAuthGuard],
    children: [
      { path: '', redirectTo: 'login', pathMatch: 'full' },
      {
        path: 'login',
        loadComponent: () => import('./features/auth/login-component/login-component')
          .then(m => m.LoginComponent),
        title: 'Login — Resolvr'
      },
      {
        path: 'register',
        loadComponent: () => import('./features/auth/register-component/register-component')
          .then(m => m.RegisterComponent),
        title: 'Register — Resolvr'
      },
      {
        path: 'verify-email',
        loadComponent: () => import('./features/auth/verify-email-component/verify-email-component')
          .then(m => m.VerifyEmailComponent),
        title: 'Verify Email — Resolvr'
      },
      {
        path: 'forgot-password',
        loadComponent: () => import('./features/auth/forgot-password-component/forgot-password-component')
          .then(m => m.ForgotPasswordComponent),
        title: 'Forgot Password — Resolvr'
      },
      {
        path: 'reset-password',
        loadComponent: () => import('./features/auth/reset-password-component/reset-password-component')
          .then(m => m.ResetPasswordComponent),
        title: 'Reset Password — Resolvr'
      },
    ]
  },

  // Main app layout routes (with sidebar)
  {
    path: 'app',
    loadComponent: () => import('./layouts/main-layout-component/main-layout-component')
      .then(m => m.MainLayoutComponent),
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard-component/dashboard-component')
          .then(m => m.DashboardComponent),
        title: 'Dashboard — Resolvr'
      },
      {
        path: 'complaints',
        loadComponent: () => import('./features/complaints/complaint-list-component/complaint-list-component')
          .then(m => m.ComplaintListComponent),
        title: 'Complaints — Resolvr'
      },
      {
        path: 'complaints/new',
        loadComponent: () => import('./features/complaints/complaint-form-component/complaint-form-component')
          .then(m => m.ComplaintFormComponent),
        title: 'New Complaint — Resolvr'
      },
      {
        path: 'complaints/:id',
        loadComponent: () => import('./features/complaints/complaint-detail-component/complaint-detail-component')
          .then(m => m.ComplaintDetailComponent),
        title: 'Complaint Detail — Resolvr'
      },
      // Admin only
      {
        path: 'admin',
        canActivate: [roleGuard(['ADMIN'])],
        children: [
          { path: '', redirectTo: 'users', pathMatch: 'full' },
          {
            path: 'users',
            loadComponent: () => import('./features/admin/user-management-component/user-management-component')
              .then(m => m.UserManagementComponent),
            title: 'User Management — Resolvr'
          },
          {
            path: 'users/:id',
            loadComponent: () => import('./features/admin/user-detail-component/user-detail-component')
              .then(m => m.UserDetailComponent),
            title: 'User Detail — Resolvr'
          },
          {
            path: 'regions',
            loadComponent: () => import('./features/admin/region-district-component/region-district-component')
              .then(m => m.RegionDistrictComponent),
            title: 'Regions & Districts — Resolvr'
          },
        ]
      },
    ]
  },

  // Fallback
  { path: '**', redirectTo: '/app/dashboard' }
];
