import {Component, inject, signal} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {Auth} from '../../../core/auth';
import {RouterLink} from '@angular/router';

@Component({
  selector: 'app-forgot-password-component',
  imports: [
    RouterLink,
    ReactiveFormsModule
  ],
  templateUrl: './forgot-password-component.html',
  styleUrl: './forgot-password-component.css',
})
export class ForgotPasswordComponent {
  private readonly fb   = inject(FormBuilder);
  private readonly auth = inject(Auth);

  loading   = signal(false);
  submitted = signal(false);

  form = this.fb.group({ email: ['', [Validators.required, Validators.email]] });

  onSubmit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.auth.forgotPassword(this.form.value.email!).subscribe({
      next: () => { this.loading.set(false); this.submitted.set(true); },
      error: () => { this.loading.set(false); this.submitted.set(true); }, // Always show success
    });
  }
}
