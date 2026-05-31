import {Component, input, output} from '@angular/core';

@Component({
  selector: 'app-confirm-dialog-component',
  imports: [],
  templateUrl: './confirm-dialog-component.html',
  styleUrl: './confirm-dialog-component.css',
})
export class ConfirmDialogComponent {
  title        = input.required<string>();
  message      = input.required<string>();
  confirmLabel = input('Confirm');
  confirmClass = input('btn-primary');
  loading      = input(false);

  confirmed = output<void>();
  cancelled  = output<void>();
}
