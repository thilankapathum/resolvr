import {Component, input} from '@angular/core';
import {ComplaintStatus, STATUS_BADGE_CLASS, STATUS_LABELS} from '../../core/models/models';

@Component({
  selector: 'app-status-badge-component',
  imports: [],
  templateUrl: './status-badge-component.html',
  styleUrl: './status-badge-component.css',
})
export class StatusBadgeComponent {
  status = input.required<ComplaintStatus>();
  label    = () => STATUS_LABELS[this.status()];
  badgeClass = () => STATUS_BADGE_CLASS[this.status()];
}
