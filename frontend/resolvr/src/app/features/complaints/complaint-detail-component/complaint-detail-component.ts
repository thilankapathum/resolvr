import {Component, computed, inject, signal} from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {ComplaintService} from '../complaint-service';
import {Auth} from '../../../core/auth';
import { ToastService } from "../../../shared/toast-service";
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {
  AUDIT_ACTION_LABELS,
  ComplaintResponse,
  ROLE_LABELS,
  STATUS_LABELS,
  UserResponse, UserRole
} from '../../../core/models/models';
import {ConfirmDialogComponent} from '../../../shared/confirm-dialog-component/confirm-dialog-component';
import {DatePipe} from '@angular/common';
import {LoadingSpinnerComponent} from '../../../shared/loading-spinner-component/loading-spinner-component';
import {StatusBadgeComponent} from '../../../shared/status-badge-component/status-badge-component';
import {PriorityBadgeComponent} from '../../../shared/priority-badge-component/priority-badge-component';
import {UserService} from '../../admin/user-service';

@Component({
  selector: 'app-complaint-detail-component',
  imports: [
    ConfirmDialogComponent,
    DatePipe,
    ReactiveFormsModule,
    LoadingSpinnerComponent,
    RouterLink,
    StatusBadgeComponent,
    PriorityBadgeComponent
  ],
  templateUrl: './complaint-detail-component.html',
  styleUrl: './complaint-detail-component.css',
})
export class ComplaintDetailComponent {
  private readonly route         = inject(ActivatedRoute);
  private readonly complaintSvc  = inject(ComplaintService);
  private readonly auth          = inject(Auth);
  private readonly toast         = inject(ToastService);
  private readonly fb            = inject(FormBuilder);
  private readonly userSvc = inject(UserService);

  protected readonly roleLabels = ROLE_LABELS;

  loading        = signal(true);
  actionLoading  = signal(false);
  complaint      = signal<ComplaintResponse | null>(null);

  availableEngineers = signal<UserResponse[]>([]);
  selectedEngineerId = signal<number | null>(null);
  escalateNotes = signal<string>('');

  reopenAssigneeId = signal<number | null>(null);
  reopenNotes      = signal<string>('');
  availableAssignees = signal<UserResponse[]>([]);

  // Reverses the audit logs to show the most recent at the top
  sortedAuditLogs = computed(() => {
    const logs = this.complaint()?.auditLogs;
    if (!logs) return [];
    return [...logs].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  });

  msisdnList = computed(() => {
    const msisdns = this.complaint()?.msisdns;
    if (!msisdns) return [];

    // Splits by comma, trims whitespace, and removes empty elements
    return msisdns.split(',').map(m => m.trim()).filter(m => m.length > 0);
  });

  showAnalysisForm   = signal(false);
  showSolutionForm   = signal(false);
  showResolveDialog  = signal(false);
  showCloseDialog    = signal(false);
  showReopenDialog   = signal(false);
  showEscalateDialog = signal(false);

  readonly statusLabels = STATUS_LABELS;
  readonly auditLabels  = AUDIT_ACTION_LABELS;

  analysisForm = this.fb.group({
    content:          ['', Validators.required],
    servingSitesCells: [''],
    coverageQuality:   [''],
  });

  solutionForm = this.fb.group({
    content:            ['', Validators.required],
    solutionTargetDate: [''],
    remarks:            [''],
  });

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loadComplaint(id);
  }

  loadComplaint(id: number) {
    this.loading.set(true);
    this.complaintSvc.getComplaint(id).subscribe({
      next: c => { this.complaint.set(c); this.loading.set(false); },
      error: () => { this.loading.set(false); this.toast.error('Failed to load complaint.'); },
    });
  }

  get id() { return this.complaint()!.id; }

  isPastTarget() {
    return this.complaint() && new Date(this.complaint()!.targetDate) < new Date();
  }

  // ── Permission helpers ────────────────────────────────────────
  isAssignee() {
    return this.complaint()?.assignedToId === this.auth.currentUser()?.id;
  }

  canStart()       { return this.isAssignee() && this.complaint()?.status === 'NOT_STARTED'; }
  canAddAnalysis() { return this.isAssignee() && ['IN_PROGRESS','ESCALATED_TO_ENGINEER'].includes(this.complaint()?.status ?? ''); }
  canAddSolution() { return this.canAddAnalysis() && (this.complaint()?.analysisEntries.length ?? 0) > 0; }
  canMarkResolved(){ return this.isAssignee() && ['IN_PROGRESS','ESCALATED_TO_ENGINEER'].includes(this.complaint()?.status ?? ''); }
  canEscalate()    { return this.auth.isTO() && this.isAssignee() && this.complaint()?.status === 'IN_PROGRESS'; }
  canClose()       { return this.auth.isManager() && this.complaint()?.status === 'RESOLVED'; }
  canReopen()      { return this.auth.isManager() && this.complaint()?.status === 'RESOLVED'; }

  // ── Actions ───────────────────────────────────────────────────
  startComplaint() {
    this.actionLoading.set(true);
    this.complaintSvc.startComplaint(this.id).subscribe({
      next: c  => { this.complaint.set(c); this.actionLoading.set(false); this.toast.success('Complaint started.'); },
      error: e => { this.actionLoading.set(false); this.toast.error(e.error?.message ?? 'Action failed.'); },
    });
  }

  submitAnalysis() {
    if (this.analysisForm.invalid) { this.analysisForm.markAllAsTouched(); return; }
    this.actionLoading.set(true);
    this.complaintSvc.addAnalysis(this.id, this.analysisForm.value as any).subscribe({
      next: c  => { this.complaint.set(c); this.actionLoading.set(false); this.showAnalysisForm.set(false); this.analysisForm.reset(); this.toast.success('Analysis saved.'); },
      error: e => { this.actionLoading.set(false); this.toast.error(e.error?.message ?? 'Failed to save analysis.'); },
    });
  }

  submitSolution() {
    if (this.solutionForm.invalid) { this.solutionForm.markAllAsTouched(); return; }
    this.actionLoading.set(true);
    this.complaintSvc.addSolution(this.id, this.solutionForm.value as any).subscribe({
      next: c  => { this.complaint.set(c); this.actionLoading.set(false); this.showSolutionForm.set(false); this.solutionForm.reset(); this.toast.success('Solution saved.'); },
      error: e => { this.actionLoading.set(false); this.toast.error(e.error?.message ?? 'Failed to save solution.'); },
    });
  }

  markResolved() {
    this.actionLoading.set(true);
    this.complaintSvc.markResolved(this.id, true).subscribe({
      next: c  => { this.complaint.set(c); this.actionLoading.set(false); this.showResolveDialog.set(false); this.toast.success('Complaint marked as resolved.'); },
      error: e => { this.actionLoading.set(false); this.toast.error(e.error?.message ?? 'Action failed.'); },
    });
  }

  closeComplaint() {
    this.actionLoading.set(true);
    this.complaintSvc.closeComplaint(this.id).subscribe({
      next: c  => { this.complaint.set(c); this.actionLoading.set(false); this.showCloseDialog.set(false); this.toast.success('Complaint closed.'); },
      error: e => { this.actionLoading.set(false); this.toast.error(e.error?.message ?? 'Action failed.'); },
    });
  }

  openEscalateDialog() {
    const districtId = this.complaint()?.districtId;
    if (!districtId) return;

    // Load engineers for this complaint's district
    this.userSvc.getAssigners(districtId).subscribe({
      next: users => {
        // Filter to engineers only
        const engineers = users.filter(u => u.role === 'ENGINEER');
        this.availableEngineers.set(engineers);
        this.selectedEngineerId.set(null);
        this.escalateNotes.set('');
        this.showEscalateDialog.set(true);
      },
      error: () => this.toast.error('Failed to load engineers for this district.'),
    });
  }

  escalateToEngineer() {
    const engineerId = this.selectedEngineerId();
    if (!engineerId) {
      this.toast.error('Please select an engineer.');
      return;
    }
    this.actionLoading.set(true);
    this.complaintSvc.escalateToEngineer(this.id, engineerId, this.escalateNotes()).subscribe({
      next: c  => {
        this.complaint.set(c);
        this.actionLoading.set(false);
        this.showEscalateDialog.set(false);
        this.toast.success('Complaint escalated to engineer.');
      },
      error: e => {
        this.actionLoading.set(false);
        this.toast.error(e.error?.message ?? 'Escalation failed.');
      },
    });
  }

  openReopenDialog() {
    const districtId = this.complaint()?.districtId;
    if (districtId) {
      this.userSvc.getAssigners(districtId).subscribe({
        next: users => this.availableAssignees.set(users),
      });
    }
    this.reopenAssigneeId.set(null);
    this.reopenNotes.set('');
    this.showReopenDialog.set(true);
  }

  reopenComplaint() {
    const assignedToId = this.reopenAssigneeId();
    const notes = this.reopenNotes();
    if (!assignedToId || !notes) {
      this.toast.error('Please select an assignee and provide a reason.');
      return;
    }
    this.actionLoading.set(true);
    this.complaintSvc.reopenComplaint(this.id, assignedToId, notes).subscribe({
      next: c => {
        this.complaint.set(c);
        this.actionLoading.set(false);
        this.showReopenDialog.set(false);
        this.toast.success('Complaint re-opened and re-assigned.');
      },
      error: e => {
        this.actionLoading.set(false);
        this.toast.error(e.error?.message ?? 'Failed to re-open complaint.');
      },
    });
  }

  copyToClipboard(text: string) {
    navigator.clipboard.writeText(text).then(() => {
      this.toast.success('Copied to clipboard'); // Assuming you have a toast service
    }).catch(err => {
      console.error('Failed to copy: ', err);
    });
  }

  getRoleLabel(role: string | null): string {
    if (!role) return '';
    return this.roleLabels[role as UserRole] ?? role;
  }
}
