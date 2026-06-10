import {Component, inject, OnInit, signal} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {AdminService} from '../../admin/admin-service';
import {ComplaintService} from '../complaint-service';
import {Auth} from '../../../core/auth';
import {Router, RouterLink} from "@angular/router";
import {ToastService} from '../../../shared/toast-service';
import {DistrictResponse, UserResponse, ROLE_LABELS} from '../../../core/models/models';
import {PageHeaderComponent} from '../../../shared/page-header-component/page-header-component';
import {UserService} from '../../admin/user-service';
import {debounceTime, distinctUntilChanged, of, Subject, switchMap} from 'rxjs';

@Component({
  selector: 'app-complaint-form-component',
  imports: [
    PageHeaderComponent,
    ReactiveFormsModule,
    RouterLink
  ],
  templateUrl: './complaint-form-component.html',
  styleUrl: './complaint-form-component.css',
})
export class ComplaintFormComponent implements OnInit {
  private readonly fb           = inject(FormBuilder);
  private readonly adminSvc     = inject(AdminService);
  private readonly userSvc = inject(UserService);
  private readonly complaintSvc = inject(ComplaintService);
  private readonly auth         = inject(Auth);
  private readonly router       = inject(Router);
  private readonly toast        = inject(ToastService);

  readonly ROLE_LABELS = ROLE_LABELS;

  step        = signal(0);
  loading     = signal(false);
  submitError = signal('');

  districts      = signal<DistrictResponse[]>([]);
  assignableUsers = signal<UserResponse[]>([]);
  sameAsContact = signal(false);

  // ── Raiser autocomplete ────────────────────────────────────────
  raiserSuggestions = signal<string[]>([]);
  showRaiserDropdown = signal(false);
  private raiserInput$ = new Subject<string>();

  form = this.fb.group({
    // General
    districtId:   ['', Validators.required],
    raisedBy:     ['', Validators.required],
    priority:     ['', Validators.required],
    assignedToId: [''],
    // Customer
    customerName:    ['', Validators.required],
    contactNumber:   ['', Validators.required],
    msisdns:         ['', Validators.required],
    address:         [''],
    latitude:  [null as number | null, Validators.required],
    longitude: [null as number | null, Validators.required],
    issueCategory:   ['', Validators.required],
    issueDescription:['', Validators.required],
    issueDuration:   [''],
    lastExperienced: [''],
    technology:      [''],
    additionalInfo:  [''],
    // Device
    deviceType:  [''],
    signalBars:  [null as number | null],
    usingVpnApn: [''],
  });

  ngOnInit() {
    this.adminSvc.getDistricts().subscribe(d => this.districts.set(d));

    // Load all assigners initially (no district selected)
    this.userSvc.getAssigners(0).subscribe({
      next: data => this.assignableUsers.set(data),
      error: err => console.error(err),
    });

    // Raiser autocomplete — debounced to avoid firing on every keystroke
    this.raiserInput$.pipe(
      debounceTime(250),
      distinctUntilChanged(),
      switchMap(q => q.length >= 2 ? this.complaintSvc.getRaiserSuggestions(q) : of([]))
    ).subscribe(suggestions => {
      this.raiserSuggestions.set(suggestions);
      this.showRaiserDropdown.set(suggestions.length > 0);
    });

    // Reload assigners when district changes
    this.form.get('districtId')?.valueChanges.subscribe(districtId => {
      const id = districtId ? Number(districtId) : 0;

      // Clear the current assignee selection since they may not belong to the new district
      this.form.get('assignedToId')?.setValue('');

      this.userSvc.getAssigners(id).subscribe({
        next: data => this.assignableUsers.set(data),
        error: err => console.error(err),
      });
    });

    // Keep MSISDN in sync with contact number when checkbox is ticked
    this.form.get('contactNumber')?.valueChanges.subscribe(value => {
      if (this.sameAsContact()) {
        this.form.get('msisdns')?.setValue(value ?? '', { emitEvent: false });
      }
    });
  }

  // ── Raiser autocomplete handlers ──────────────────────────────
  onRaiserInput(event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.raiserInput$.next(value);
    if (value.length < 2) {
      this.raiserSuggestions.set([]);
      this.showRaiserDropdown.set(false);
    }
  }

  selectRaiser(name: string) {
    this.form.patchValue({ raisedBy: name });
    this.showRaiserDropdown.set(false);
    this.raiserSuggestions.set([]);
  }

  hideRaiserDropdown() {
    // Small delay so a click on a suggestion registers before the blur fires
    setTimeout(() => this.showRaiserDropdown.set(false), 150);
  }

  isInvalid(field: string) {
    const c = this.form.get(field);
    return c?.invalid && c?.touched;
  }

  selectedDistrictName() {
    const id = Number(this.form.value.districtId);
    return this.districts().find(d => d.id === id)?.name ?? '—';
  }

  onSubmit() {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.submitError.set('Please fill all required fields before submitting.');
      return;
    }

    this.loading.set(true);
    this.submitError.set('');

    const raw = this.form.getRawValue(); // ← changed from this.form.value
    const payload = {
      ...raw,
      districtId:   Number(raw.districtId),
      assignedToId: raw.assignedToId ? Number(raw.assignedToId) : null,
      signalBars:   raw.signalBars != null ? Number(raw.signalBars) : null,
      usingVpnApn:  raw.usingVpnApn === 'true' ? true : raw.usingVpnApn === 'false' ? false : null,
      latitude:     raw.latitude != null ? Number(raw.latitude) : null,
      longitude:    raw.longitude != null ? Number(raw.longitude) : null,
      lastExperienced: raw.lastExperienced || null,
      technology:   raw.technology || null,
      deviceType:   raw.deviceType || null,
    };

    this.complaintSvc.createComplaint(payload).subscribe({
      next: complaint => {
        this.loading.set(false);
        this.toast.success(`Complaint ${complaint.refNumber} logged successfully.`);
        this.router.navigate(['/app/complaints', complaint.refNumber]);
      },
      error: err => {
        this.loading.set(false);
        this.submitError.set(err.error?.message ?? 'Failed to submit complaint. Please try again.');
      },
    });
  }

  toggleSameAsContact(event: Event) {
    const checked = (event.target as HTMLInputElement).checked;
    this.sameAsContact.set(checked);

    if (checked) {
      const contact = this.form.get('contactNumber')?.value ?? '';
      this.form.get('msisdns')?.setValue(contact);
      this.form.get('msisdns')?.disable();
    } else {
      this.form.get('msisdns')?.enable();
      this.form.get('msisdns')?.setValue('');
    }
  }

  assignedUserName(): string {
    const id = Number(this.form.getRawValue().assignedToId);
    return this.assignableUsers().find(u => u.id === id)?.fullName ?? '—';
  }
}
