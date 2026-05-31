import {Component, input} from '@angular/core';
import {ComplaintPriority, PRIORITY_BADGE_CLASS, PRIORITY_LABELS} from '../../core/models/models';

@Component({
  selector: 'app-priority-badge-component',
  imports: [],
  templateUrl: './priority-badge-component.html',
  styleUrl: './priority-badge-component.css',
})
export class PriorityBadgeComponent {
  priority = input.required<ComplaintPriority>();
  label      = () => PRIORITY_LABELS[this.priority()];
  badgeClass = () => PRIORITY_BADGE_CLASS[this.priority()];
}
