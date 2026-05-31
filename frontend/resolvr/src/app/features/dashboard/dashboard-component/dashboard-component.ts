import {Component, inject, OnInit, signal} from '@angular/core';
import {Auth} from '../../../core/auth';
import {ComplaintService} from '../../complaints/complaint-service';
import {ComplaintResponse} from '../../../core/models/models';
import {StatusBadgeComponent} from '../../../shared/status-badge-component/status-badge-component';
import {PriorityBadgeComponent} from '../../../shared/priority-badge-component/priority-badge-component';
import {DatePipe} from '@angular/common';
import {RouterLink} from '@angular/router';

@Component({
  selector: 'app-dashboard-component',
  imports: [
    StatusBadgeComponent,
    PriorityBadgeComponent,
    DatePipe,
    RouterLink
  ],
  templateUrl: './dashboard-component.html',
  styleUrl: './dashboard-component.css',
})
export class DashboardComponent implements OnInit{
  readonly auth = inject(Auth);
  private readonly complaintSvc = inject(ComplaintService);

  loading          = signal(true);
  recentComplaints = signal<ComplaintResponse[]>([]);

  stats = signal<{ label: string; value: number; colorClass: string }[]>([]);

  ngOnInit() { this.loadData(); }

  loadData() {
    this.loading.set(true);
    this.complaintSvc.getComplaints(0, 10, 'createdAt,desc').subscribe({
      next: page => {
        const complaints = page.content;
        this.recentComplaints.set(complaints);
        this.buildStats(page.totalElements, complaints);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  buildStats(total: number, complaints: ComplaintResponse[]) {
    const open     = complaints.filter(c => !['CLOSED','RESOLVED'].includes(c.status)).length;
    const resolved = complaints.filter(c => c.status === 'RESOLVED').length;
    const overdue  = complaints.filter(c =>
      !['CLOSED','RESOLVED'].includes(c.status) && new Date(c.targetDate) < new Date()
    ).length;

    this.stats.set([
      { label: 'Total Complaints', value: total,    colorClass: 'text-primary' },
      { label: 'Open',             value: open,     colorClass: 'text-warning' },
      { label: 'Pending Closure',  value: resolved, colorClass: 'text-success' },
      { label: 'Overdue',          value: overdue,  colorClass: overdue > 0 ? 'text-error' : 'text-base-content' },
    ]);
  }

  timeOfDay() {
    const h = new Date().getHours();
    return h < 12 ? 'morning' : h < 17 ? 'afternoon' : 'evening';
  }

  firstName() {
    return this.auth.currentUser()?.fullName.split(' ')[0] ?? 'there';
  }

  roleLabel() {
    const role = this.auth.currentUser()?.role;
    return role?.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase()) ?? '';
  }

  isPast(date: string) { return new Date(date) < new Date(); }
}
