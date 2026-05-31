import {Component, inject, signal} from '@angular/core';
import {HttpErrorResponse} from '@angular/common/http';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router, RouterLink} from '@angular/router';
import {Auth} from '../../../core/auth';

@Component({
  selector: 'app-login-component',
  imports: [
    ReactiveFormsModule,
    RouterLink
  ],
  templateUrl: './login-component.html',
  styleUrl: './login-component.css',
})
export class LoginComponent {
  private readonly fb     = inject(FormBuilder);
  private readonly auth   = inject(Auth);
  private readonly router = inject(Router);

  loading      = signal(false);
  errorMessage = signal('');

  form = this.fb.group({
    email:    ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  onSubmit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.errorMessage.set('');

    const { email, password } = this.form.value;
    this.auth.login(email!, password!).subscribe({
      next: () => this.router.navigate(['/app/dashboard']),
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message ?? 'Login failed. Please try again.');
      },
    });
  }
}
