import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  username = '';
  password = '';
  error = '';

  constructor(private authService: AuthService, private router: Router) {}

  onLogin() {
    const credentials = { username: this.username, password: this.password };
    console.log('Slanje kredencijala:', credentials);
    this.authService.login(credentials).subscribe({
      next: (res) => {
        console.log('Login response:', res);
        this.authService.saveToken(res.token);
        this.router.navigate(['/users']);
      },
      error: (err) => {
        console.error('Login error:', err);
        console.error('Error details:', err.error);
        this.error = err.error?.error || err.message || 'Greška pri logovanju';
      }
    });
  }
}