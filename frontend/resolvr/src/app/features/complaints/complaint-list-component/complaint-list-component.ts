import {Component, inject, OnDestroy, OnInit, signal} from '@angular/core';
import {ComplaintService} from '../complaint-service';
import {Router, RouterLink} from '@angular/router';
import {Auth} from '../../../core/auth';
import {ComplaintResponse, ComplaintStatus, Page} from '../../../core/models/models';
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
export class ComplaintListComponent implements OnInit, OnDestroy{
  private readonly complaintSvc = inject(ComplaintService);
  private readonly router       = inject(Router);
  readonly auth                 = inject(Auth);

  Math = Math;

  loading      = signal(true);
  pagedData    = signal<Page<ComplaintResponse> | null>(null);
  currentPage  = signal(0);
  filterStatus = signal<string>('');
  searchQuery  = signal<string>('');

  // Debounce search input to avoid firing on every keystroke
  private readonly searchSubject = new Subject<string>();
  private readonly destroy$      = new Subject<void>();

  statusOptions: { value: ComplaintStatus | ''; label: string }[] = [
    { value: '',                       label: 'All' },
    { value: 'NOT_ASSIGNED',           label: 'Not Assigned' },
    { value: 'NOT_STARTED',            label: 'Not Started' },
    { value: 'IN_PROGRESS',            label: 'In Progress' },
    { value: 'ESCALATED_TO_ENGINEER',  label: 'Escalated' },
    { value: 'RESOLVED',               label: 'Resolved' },
    { value: 'CLOSED',                 label: 'Closed' },
  ];

  ngOnInit() {
    this.load();

    // Debounce search — wait 350ms after user stops typing
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
      .getComplaints(this.currentPage(), 20, this.filterStatus(), this.searchQuery())
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

  setPage(page: number) {
    this.currentPage.set(page);
    this.load();
  }

  navigateTo(id: number) {
    this.router.navigate(['/app/complaints', id]);
  }

  isPastTarget(targetDate: string) {
    return new Date(targetDate) < new Date();
  }
}
