import {Component, inject, signal} from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {Auth} from '../../../core/auth';

@Component({
  selector: 'app-verify-email-component',
  imports: [
    RouterLink
  ],
  templateUrl: './verify-email-component.html',
  styleUrl: './verify-email-component.css',
})
export class VerifyEmailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly auth  = inject(Auth);

  loading      = signal(true);
  success      = signal(false);
  errorMessage = signal('');

  ngOnInit() {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.loading.set(false);
      this.errorMessage.set('No verification token found in the URL.');
      return;
    }
    this.auth.verifyEmail(token).subscribe({
      next: () => { this.loading.set(false); this.success.set(true); },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message ?? 'Invalid or expired token.');
      },
    });
  }
}
