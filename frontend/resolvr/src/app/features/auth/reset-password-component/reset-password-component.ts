import {Component, inject, signal} from '@angular/core';
import {AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators} from '@angular/forms';
import {Auth} from '../../../core/auth';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {HttpErrorResponse} from '@angular/common/http';

function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
  const pw  = control.get('newPassword')?.value;
  const cpw = control.get('confirmPassword')?.value;
  return pw === cpw ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-reset-password-component',
  imports: [
    RouterLink,
    ReactiveFormsModule
  ],
  templateUrl: './reset-password-component.html',
  styleUrl: './reset-password-component.css',
})
export class ResetPasswordComponent {
  private readonly fb    = inject(FormBuilder);
  private readonly auth  = inject(Auth);
  private readonly route = inject(ActivatedRoute);

  loading      = signal(false);
  done         = signal(false);
  errorMessage = signal('');
  private token = '';

  form = this.fb.group({
    newPassword:     ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required],
  }, { validators: passwordMatchValidator });

  ngOnInit() {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!this.token) this.errorMessage.set('Invalid reset link. Please request a new one.');
  }

  onSubmit() {
    if (this.form.invalid || !this.token) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.auth.resetPassword(this.token, this.form.value.newPassword!).subscribe({
      next: () => { this.loading.set(false); this.done.set(true); },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message ?? 'Reset failed. The link may have expired.');
      },
    });
  }
}
