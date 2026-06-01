import {Component, computed, inject, OnDestroy, OnInit, signal} from '@angular/core';
import {ComplaintService} from '../complaint-service';
import {Router, RouterLink} from '@angular/router';
import {Auth} from '../../../core/auth';
import {
  ComplaintResponse,
  ComplaintStatus,
  Page,
  ROLE_LABELS,
  STATUS_LABELS,
  UserRole
} from '../../../core/models/models';
import {PageHeaderComponent} from '../../../shared/page-header-component/page-header-component';
import {LoadingSpinnerComponent} from '../../../shared/loading-spinner-component/loading-spinner-component';
import {PriorityBadgeComponent} from '../../../shared/priority-badge-component/priority-badge-component';
import {StatusBadgeComponent} from '../../../shared/status-badge-component/status-badge-component';
import {DatePipe} from '@angular/common';
import {debounceTime, distinctUntilChanged, Subject, takeUntil} from 'rxjs';
import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-complaint-list-component',
  imports: [
    PageHeaderComponent,
    RouterLink,
    LoadingSpinnerComponent,
    PriorityBadgeComponent,
    StatusBadgeComponent,
    DatePipe,
    FormsModule
  ],
  templateUrl: './complaint-list-component.html',
  styleUrl: './complaint-list-component.css',
})
export class ComplaintListComponent implements OnInit, OnDestroy {
  private readonly complaintSvc = inject(ComplaintService);
  private readonly router       = inject(Router);
  readonly auth                 = inject(Auth);
  protected readonly roleLabels = ROLE_LABELS;

  Math = Math;

  loading      = signal(true);
  pagedData    = signal<Page<ComplaintResponse> | null>(null);
  currentPage  = signal(0);
  pageSize     = signal(10);
  filterStatus = signal<string>('');
  searchQuery  = signal<string>('');

  private readonly searchSubject = new Subject<string>();
  private readonly destroy$      = new Subject<void>();

  pageSizeOptions = [10, 20, 50, 100];

  statusOptions: { value: ComplaintStatus | ''; label: string }[] = [
    { value: '',                       label: 'All' },
    { value: 'NOT_ASSIGNED',           label: 'Not Assigned' },
    { value: 'NOT_STARTED',            label: 'Not Started' },
    { value: 'IN_PROGRESS',            label: 'In Progress' },
    { value: 'ESCALATED_TO_ENGINEER',  label: 'Escalated' },
    { value: 'RESOLVED',               label: 'Resolved' },
    { value: 'CLOSED',                 label: 'Closed' },
  ];

  // Computed visible page numbers for pagination bar
  visiblePages = computed(() => {
    const total = this.pagedData()?.totalPages ?? 0;
    const current = this.currentPage();
    if (total <= 7) return Array.from({ length: total }, (_, i) => i);

    const pages: (number | '...')[] = [];
    if (current <= 3) {
      pages.push(0, 1, 2, 3, 4, '...', total - 1);
    } else if (current >= total - 4) {
      pages.push(0, '...', total - 5, total - 4, total - 3, total - 2, total - 1);
    } else {
      pages.push(0, '...', current - 1, current, current + 1, '...', total - 1);
    }
    return pages;
  });

  ngOnInit() {
    this.load();
    this.searchSubject.pipe(
      debounceTime(350),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    ).subscribe(query => {
      this.searchQuery.set(query);
      this.currentPage.set(0);
      this.load();
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  load() {
    this.loading.set(true);
    this.complaintSvc
      .getComplaints(this.currentPage(), this.pageSize(), this.filterStatus(), this.searchQuery())
      .subscribe({
        next: data => { this.pagedData.set(data); this.loading.set(false); },
        error: ()   => { this.loading.set(false); },
      });
  }

  onSearchInput(value: string) {
    this.searchSubject.next(value);
  }

  setStatus(s: string) {
    this.filterStatus.set(s);
    this.currentPage.set(0);
    this.load();
  }

  setPage(page: number | '...') {
    if (page === '...') return;
    this.currentPage.set(page);
    this.load();
  }

  setPageSize(size: number) {
    this.pageSize.set(size);
    this.currentPage.set(0);
    this.load();
  }

  navigateTo(id: number) {
    this.router.navigate(['/app/complaints', id]);
  }

  isPastTarget(targetDate: string) {
    return new Date(targetDate) < new Date();
  }

  protected readonly STATUS_LABELS = STATUS_LABELS;

  getRoleLabel(role: string | null): string {
    if (!role) return '';
    return this.roleLabels[role as UserRole] ?? role;
  }

  getStatusLabel(status: string | null): string {
    return this.STATUS_LABELS[status as ComplaintStatus] ?? status ?? '';
  }
}
