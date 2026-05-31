import {Component, inject, OnInit, signal} from '@angular/core';
import {AdminService} from '../admin-service';
import {ToastService} from '../../../shared/toast-service';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {DistrictResponse, RegionResponse} from '../../../core/models/models';
import {ConfirmDialogComponent} from '../../../shared/confirm-dialog-component/confirm-dialog-component';
import {LoadingSpinnerComponent} from '../../../shared/loading-spinner-component/loading-spinner-component';
import {PageHeaderComponent} from '../../../shared/page-header-component/page-header-component';

@Component({
  selector: 'app-region-district-component',
  imports: [
    ConfirmDialogComponent,
    LoadingSpinnerComponent,
    ReactiveFormsModule,
    PageHeaderComponent
  ],
  templateUrl: './region-district-component.html',
  styleUrl: './region-district-component.css',
})
export class RegionDistrictComponent implements OnInit {

  private readonly adminSvc = inject(AdminService);
  private readonly toast    = inject(ToastService);
  private readonly fb       = inject(FormBuilder);

  loading       = signal(true);
  actionLoading = signal(false);
  regions       = signal<RegionResponse[]>([]);
  allDistricts  = signal<DistrictResponse[]>([]);
  selectedRegion   = signal<RegionResponse | null>(null);
  selectedDistricts = signal<Set<number>>(new Set());

  showNewRegionForm       = signal(false);
  showDeleteRegionDialog  = signal(false);
  deletingRegion          = signal<RegionResponse | null>(null);

  regionForm = this.fb.group({ name: ['', [Validators.required, Validators.minLength(2)]] });

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.adminSvc.getRegions().subscribe(r => { this.regions.set(r); this.loading.set(false); });
    this.adminSvc.getDistricts().subscribe(d => this.allDistricts.set(d));
  }

  selectRegion(region: RegionResponse) {
    this.selectedRegion.set(region);
    this.selectedDistricts.set(new Set(region.districts.map(d => d.id)));
  }

  toggleDistrict(id: number, event: Event) {
    const checked = (event.target as HTMLInputElement).checked;
    this.selectedDistricts.update(set => {
      const next = new Set(set);
      checked ? next.add(id) : next.delete(id);
      return next;
    });
  }

  createRegion() {
    if (this.regionForm.invalid) { this.regionForm.markAllAsTouched(); return; }
    this.actionLoading.set(true);
    this.adminSvc.createRegion(this.regionForm.value.name!).subscribe({
      next: r => {
        this.regions.update(list => [...list, r]);
        this.regionForm.reset();
        this.showNewRegionForm.set(false);
        this.actionLoading.set(false);
        this.toast.success(`Region "${r.name}" created.`);
      },
      error: e => { this.actionLoading.set(false); this.toast.error(e.error?.message ?? 'Failed to create.'); },
    });
  }

  assignDistricts() {
    const region = this.selectedRegion();
    if (!region) return;
    this.actionLoading.set(true);

    // Send the full current selection — backend will diff against existing
    this.adminSvc.assignDistrictsToRegion(region.id, Array.from(this.selectedDistricts())).subscribe({
      next: updated => {
        // Refresh everything from scratch so all district regionId values are current
        this.adminSvc.getRegions().subscribe(regions => {
          this.regions.set(regions);
          // Re-select the updated region from fresh data
          const refreshed = regions.find(r => r.id === updated.id) ?? updated;
          this.selectedRegion.set(refreshed);
          this.selectedDistricts.set(new Set(refreshed.districts.map(d => d.id)));
        });
        this.adminSvc.getDistricts().subscribe(d => this.allDistricts.set(d));
        this.actionLoading.set(false);
        this.toast.success('Districts updated successfully.');
      },
      error: e => {
        this.actionLoading.set(false);
        this.toast.error(e.error?.message ?? 'Failed to assign.');
      },
    });
  }

  confirmDeleteRegion(region: RegionResponse) {
    this.deletingRegion.set(region);
    this.showDeleteRegionDialog.set(true);
  }

  deleteRegion() {
    this.actionLoading.set(true);
    this.adminSvc.deleteRegion(this.deletingRegion()!.id).subscribe({
      next: () => {
        this.regions.update(list => list.filter(r => r.id !== this.deletingRegion()!.id));
        if (this.selectedRegion()?.id === this.deletingRegion()!.id) this.selectedRegion.set(null);
        this.actionLoading.set(false);
        this.showDeleteRegionDialog.set(false);
        this.toast.success('Region deleted.');
        this.load();
      },
      error: e => { this.actionLoading.set(false); this.toast.error(e.error?.message ?? 'Failed to delete.'); },
    });
  }
}
