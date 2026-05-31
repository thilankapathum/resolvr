import {Component, inject, OnInit, signal} from '@angular/core';
import {AdminService} from '../admin-service';
import {ToastService} from '../../../shared/toast-service';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {RegionResponse, ROLE_LABELS, UserResponse} from '../../../core/models/models';
import {ConfirmDialogComponent} from '../../../shared/confirm-dialog-component/confirm-dialog-component';
import {PageHeaderComponent} from '../../../shared/page-header-component/page-header-component';
import {LoadingSpinnerComponent} from '../../../shared/loading-spinner-component/loading-spinner-component';
import {RouterLink} from '@angular/router';
import {DatePipe} from '@angular/common';

@Component({
  selector: 'app-user-management-component',
  imports: [
    ConfirmDialogComponent,
    PageHeaderComponent,
    LoadingSpinnerComponent,
    RouterLink,
    DatePipe,
    ReactiveFormsModule
  ],
  templateUrl: './user-management-component.html',
  styleUrl: './user-management-component.css',
})
export class UserManagementComponent implements OnInit{
  private readonly adminSvc = inject(AdminService);
  private readonly toast    = inject(ToastService);
  private readonly fb       = inject(FormBuilder);

  loading      = signal(true);
  actionLoading = signal(false);
  users        = signal<UserResponse[]>([]);
  regions      = signal<RegionResponse[]>([]);
  activeTab    = signal<'all' | 'pending'>('all');

  showActivateModal   = signal(false);
  showDeactivateDialog = signal(false);
  activatingUser   = signal<UserResponse | null>(null);
  deactivatingUser = signal<UserResponse | null>(null);

  readonly roleLabels = ROLE_LABELS;

  activateForm = this.fb.group({
    role:     ['', Validators.required],
    regionId: ['', Validators.required],
  });

  filteredUsers = () => this.activeTab() === 'pending'
    ? this.users().filter(u => !u.active && u.emailVerified)
    : this.users();

  pendingCount = () => this.users().filter(u => !u.active && u.emailVerified).length;

  ngOnInit() {
    this.adminSvc.getUsers(0, 100).subscribe({
      next: page => { this.users.set(page.content); this.loading.set(false); },
      error: ()  => this.loading.set(false),
    });
    this.adminSvc.getRegions().subscribe(r => this.regions.set(r));
  }

  initials(user: UserResponse) {
    return user.fullName.split(' ').map(p => p[0]).slice(0, 2).join('').toUpperCase();
  }

  openActivateModal(user: UserResponse) {
    this.activatingUser.set(user);
    this.activateForm.reset();
    this.showActivateModal.set(true);
  }

  activateUser() {
    if (this.activateForm.invalid) { this.activateForm.markAllAsTouched(); return; }
    const { role, regionId } = this.activateForm.value;
    this.actionLoading.set(true);
    this.adminSvc.activateUser(this.activatingUser()!.id, role!, Number(regionId)).subscribe({
      next: updated => {
        this.users.update(list => list.map(u => u.id === updated.id ? updated : u));
        this.actionLoading.set(false);
        this.showActivateModal.set(false);
        this.toast.success(`${updated.fullName} has been activated as ${ROLE_LABELS[updated.role!]}.`);
      },
      error: e => { this.actionLoading.set(false); this.toast.error(e.error?.message ?? 'Failed to activate.'); },
    });
  }

  confirmDeactivate(user: UserResponse) {
    this.deactivatingUser.set(user);
    this.showDeactivateDialog.set(true);
  }

  deactivateUser() {
    this.actionLoading.set(true);
    this.adminSvc.deactivateUser(this.deactivatingUser()!.id).subscribe({
      next: updated => {
        this.users.update(list => list.map(u => u.id === updated.id ? updated : u));
        this.actionLoading.set(false);
        this.showDeactivateDialog.set(false);
        this.toast.success(`${updated.fullName} has been deactivated.`);
      },
      error: e => { this.actionLoading.set(false); this.toast.error(e.error?.message ?? 'Failed to deactivate.'); },
    });
  }

}
