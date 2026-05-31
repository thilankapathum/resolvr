import {Component, inject, signal} from '@angular/core';
import {ComplaintService} from '../complaint-service';
import {RouterLink} from '@angular/router';
import {Auth} from '../../../core/auth';
import {ComplaintResponse, ComplaintStatus, Page} from '../../../core/models/models';
import {PageHeaderComponent} from '../../../shared/page-header-component/page-header-component';
import {LoadingSpinnerComponent} from '../../../shared/loading-spinner-component/loading-spinner-component';
import {PriorityBadgeComponent} from '../../../shared/priority-badge-component/priority-badge-component';
import {StatusBadgeComponent} from '../../../shared/status-badge-component/status-badge-component';
import {DatePipe} from '@angular/common';

@Component({
  selector: 'app-complaint-list-component',
  imports: [
    PageHeaderComponent,
    RouterLink,
    LoadingSpinnerComponent,
    PriorityBadgeComponent,
    StatusBadgeComponent,
    DatePipe
  ],
  templateUrl: './complaint-list-component.html',
  styleUrl: './complaint-list-component.css',
})
export class ComplaintListComponent {
  private readonly complaintSvc = inject(ComplaintService);
  private readonly router = inject(RouterLink);
  readonly auth = inject(Auth);

  Math = Math;

  loading     = signal(true);
  pagedData   = signal<Page<ComplaintResponse> | null>(null);
  currentPage = signal(0);
  filterStatus = signal<string>('');

  statusOptions: { value: ComplaintStatus | '', label: string }[] = [
    { value: 'NOT_ASSIGNED',          label: 'Not Assigned' },
    { value: 'NOT_STARTED',           label: 'Not Started' },
    { value: 'IN_PROGRESS',           label: 'In Progress' },
    { value: 'ESCALATED_TO_ENGINEER', label: 'Escalated' },
    { value: 'RESOLVED',              label: 'Resolved' },
    { value: 'CLOSED',                label: 'Closed' },
  ];

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.complaintSvc.getComplaints(this.currentPage(), 20).subscribe({
      next: data => { this.pagedData.set(data); this.loading.set(false); },
      error: ()  => { this.loading.set(false); },
    });
  }

  setPage(page: number) { this.currentPage.set(page); this.load(); }
  setStatus(s: string)  { this.filterStatus.set(s); this.currentPage.set(0); this.load(); }

  navigateTo(id: number) { window.location.href = `/app/complaints/${id}`; }

  isPastTarget(targetDate: string) {
    return new Date(targetDate) < new Date();
  }
}
