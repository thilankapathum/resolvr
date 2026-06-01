import {Component, computed, DestroyRef, inject, signal} from '@angular/core';
import {ROLE_LABELS} from '../../core/models/models';
import {Auth} from '../../core/auth';
import {ToastContainerComponent} from '../../shared/toast-container-component/toast-container-component';
import {RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {FormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';

const SIDEBAR_STORAGE_KEY = 'resolvr_sidebar';

@Component({
  selector: 'app-main-layout-component',
  imports: [
    ToastContainerComponent,
    RouterLinkActive,
    RouterLink,
    RouterOutlet,
    CommonModule
  ],
  templateUrl: './main-layout-component.html',
  styleUrl: './main-layout-component.css',
})
export class MainLayoutComponent {
  readonly auth = inject(Auth);
  private readonly destroyRef = inject(DestroyRef);

  // Sidebar open/collapsed — persisted across sessions
  sidebarOpen  = signal<boolean>(this.loadSidebarState());

  // Reactive mobile detection — initialised immediately to avoid flicker on reload
  isMobile     = signal<boolean>(window.innerWidth < 1024);

  // Mobile: sidebar hidden by default, shown on hamburger tap
  mobileHidden = signal<boolean>(window.innerWidth < 1024);

  user = this.auth.currentUser;

  initials = computed(() => {
    const name = this.user()?.fullName ?? '';
    return name.split(' ').map(p => p[0]).slice(0, 2).join('').toUpperCase();
  });

  roleLabel = computed(() => {
    const role = this.user()?.role;
    return role ? ROLE_LABELS[role] : '';
  });

  isAdmin    = this.auth.isAdmin;
  isManager  = this.auth.isManager;
  isHead     = this.auth.isHead;
  isEngineer = this.auth.isEngineer;
  isTO       = this.auth.isTO;

  constructor() {
    const handleResize = () => {
      const mobile = window.innerWidth < 1024;
      this.isMobile.set(mobile);
      // When resizing back to desktop, always show the sidebar
      if (!mobile) {
        this.mobileHidden.set(false);
      }
    };

    window.addEventListener('resize', handleResize);

    // Automatically removes the listener when the component is destroyed
    this.destroyRef.onDestroy(() => {
      window.removeEventListener('resize', handleResize);
    });
  }

  toggleSidebar(): void {
    const next = !this.sidebarOpen();
    this.sidebarOpen.set(next);
    localStorage.setItem(SIDEBAR_STORAGE_KEY, next ? '1' : '0');
  }

  toggleMobile(): void {
    this.mobileHidden.update(v => !v);
  }

  private loadSidebarState(): boolean {
    return localStorage.getItem(SIDEBAR_STORAGE_KEY) !== '0';
  }
}
