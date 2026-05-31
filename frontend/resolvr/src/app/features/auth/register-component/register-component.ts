import {Component, inject, signal} from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import {Auth} from '../../../core/auth';
import {HttpErrorResponse} from '@angular/common/http';
import {RouterLink} from '@angular/router';

function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
  const password        = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;
  return password === confirmPassword ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-register-component',
  imports: [
    RouterLink,
    ReactiveFormsModule
  ],
  templateUrl: './register-component.html',
  styleUrl: './register-component.css',
})
export class RegisterComponent {
  private readonly fb   = inject(FormBuilder);
  private readonly auth = inject(Auth);

  loading      = signal(false);
  errorMessage = signal('');
  submitted    = signal(false);

  form: FormGroup = new FormGroup(
    {
      fullName:        this.fb.control('', [Validators.required, Validators.minLength(2)]),
      email:           this.fb.control('', [Validators.required, Validators.email]),
      password:        this.fb.control('', [Validators.required, Validators.minLength(8)]),
      confirmPassword: this.fb.control('', Validators.required),
    },
    { validators: passwordMatchValidator }
  );

  isInvalid(field: string) {
    const c = this.form.get(field);
    return c?.invalid && c?.touched;
  }

  onSubmit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.errorMessage.set('');
    const { fullName, email, password } = this.form.value;
    this.auth.register(fullName!, email!, password!).subscribe({
      next: () => { this.loading.set(false); this.submitted.set(true); },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message ?? 'Registration failed. Please try again.');
      },
    });
  }
}
