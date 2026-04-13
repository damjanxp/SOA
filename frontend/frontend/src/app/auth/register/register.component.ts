import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
  username = '';
  password = '';
  email = '';
  role = 'tourist';
  error = '';
  success = '';

  constructor(private authService: AuthService, private router: Router) {}

  onRegister() {
    this.authService.register({ 
      username: this.username, 
      password: this.password, 
      email: this.email, 
      role: this.role 
    }).subscribe({
      next: () => {
        this.success = 'Registracija uspešna! Možete se ulogovati.';
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: (err) => {
        console.error('Registracija greška:', err);
        this.error = err.error?.error || err.message || 'Greška pri registraciji';
      }
    });
  }
}