import {Component, inject, signal} from '@angular/core';
import {ROLE_LABELS} from '../../core/models/models';
import {Auth} from '../../core/auth';
import {ToastContainerComponent} from '../../shared/toast-container-component/toast-container-component';
import {RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-main-layout-component',
  imports: [
    ToastContainerComponent,
    RouterLinkActive,
    RouterLink,
    RouterOutlet,
    FormsModule
  ],
  templateUrl: './main-layout-component.html',
  styleUrl: './main-layout-component.css',
})
export class MainLayoutComponent {
  readonly auth = inject(Auth);
  drawerOpen = signal(false);

  pageTitle = () => 'Complaint Management';

  initials = () => {
    const name = this.auth.currentUser()?.fullName ?? '';
    return name.split(' ').map(p => p[0]).slice(0, 2).join('').toUpperCase();
  };

  roleLabel = () => {
    const role = this.auth.currentUser()?.role;
    return role ? ROLE_LABELS[role] : '';
  };
}
