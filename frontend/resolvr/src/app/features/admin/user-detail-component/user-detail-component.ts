import {Component, inject, OnInit, signal} from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {AdminService} from '../admin-service';
import {ToastService} from '../../../shared/toast-service';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {DistrictResponse, RegionResponse, ROLE_LABELS, UserResponse} from '../../../core/models/models';
import {LoadingSpinnerComponent} from '../../../shared/loading-spinner-component/loading-spinner-component';

@Component({
  selector: 'app-user-detail-component',
  imports: [
    ReactiveFormsModule,
    LoadingSpinnerComponent,
    RouterLink
  ],
  templateUrl: './user-detail-component.html',
  styleUrl: './user-detail-component.css',
})
export class UserDetailComponent implements OnInit{
  private readonly route    = inject(ActivatedRoute);
  private readonly adminSvc = inject(AdminService);
  private readonly toast    = inject(ToastService);
  private readonly fb       = inject(FormBuilder);

  loading       = signal(true);
  actionLoading = signal(false);
  user          = signal<UserResponse | null>(null);
  regions       = signal<RegionResponse[]>([]);
  allDistricts  = signal<DistrictResponse[]>([]);
  assignedDistrictIds = signal<Set<number>>(new Set());

  readonly roleLabels = ROLE_LABELS;

  roleForm = this.fb.group({
    role:     ['', Validators.required],
    regionId: [''],
  });

  passwordForm = this.fb.group({
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
  });

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.adminSvc.getUserById(id).subscribe({
      next: u => {
        this.user.set(u);
        this.loading.set(false);
        this.roleForm.patchValue({ role: u.role ?? '', regionId: u.regionId?.toString() ?? '' });
        this.assignedDistrictIds.set(new Set(u.districtIds));
      },
    });
    this.adminSvc.getRegions().subscribe(r => this.regions.set(r));
    this.adminSvc.getDistricts().subscribe(d => this.allDistricts.set(d));
  }

  toggleDistrict(id: number, event: Event) {
    const checked = (event.target as HTMLInputElement).checked;
    this.assignedDistrictIds.update(set => {
      const next = new Set(set);
      checked ? next.add(id) : next.delete(id);
      return next;
    });
  }

  updateRole() {
    if (this.roleForm.invalid) return;
    this.actionLoading.set(true);
    const { role, regionId } = this.roleForm.value;
    this.adminSvc.updateUserRole(this.user()!.id, role!, regionId ? Number(regionId) : undefined).subscribe({
      next: u => { this.user.set(u); this.actionLoading.set(false); this.toast.success('Role updated.'); },
      error: e => { this.actionLoading.set(false); this.toast.error(e.error?.message ?? 'Update failed.'); },
    });
  }

  saveDistricts() {
    this.actionLoading.set(true);
    this.adminSvc.assignDistricts(this.user()!.id, Array.from(this.assignedDistrictIds())).subscribe({
      next: u => { this.user.set(u); this.actionLoading.set(false); this.toast.success('Districts saved.'); },
      error: e => { this.actionLoading.set(false); this.toast.error(e.error?.message ?? 'Failed to save.'); },
    });
  }

  resetPassword() {
    if (this.passwordForm.invalid) { this.passwordForm.markAllAsTouched(); return; }
    this.actionLoading.set(true);
    this.adminSvc.adminResetPassword(this.user()!.id, this.passwordForm.value.newPassword!).subscribe({
      next: () => { this.passwordForm.reset(); this.actionLoading.set(false); this.toast.success('Password reset and email sent.'); },
      error: e => { this.actionLoading.set(false); this.toast.error(e.error?.message ?? 'Reset failed.'); },
    });
  }
}
