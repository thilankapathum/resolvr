import {Component, inject} from '@angular/core';
import {ToastService} from '../toast-service';

@Component({
  selector: 'app-toast-container-component',
  imports: [],
  templateUrl: './toast-container-component.html',
  styleUrl: './toast-container-component.css',
})
export class ToastContainerComponent {
  readonly toastService = inject(ToastService);
}
