import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../auth/auth.service';

interface User {
  id: string;
  username: string;
  email: string;
  role: string;
  isBlocked: boolean;
}

@Component({
  selector: 'app-users',
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.scss'],
})
export class UsersComponent implements OnInit {
  users: User[] = [];
  loading = true;
  error = '';

  constructor(
    private http: HttpClient,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    const token = this.authService.getToken();
    console.log('Token:', token);
    if (!token) {
      this.error = 'No token found';
      this.loading = false;
      return;
    }

    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`,
    });

    this.http
      .get<any>('http://localhost:8081/api/users', { headers })
      .subscribe({
        next: (res) => {
          console.log('Users response:', res);
          // Backend vraća {data: [...]} struktura
          const data = res.data || res;
          this.users = Array.isArray(data) ? data : [];
          console.log('Users loaded:', this.users);
          this.loading = false;
        },
        error: (err) => {
          console.error('Users error:', err);
          this.error = err.error?.error || 'Greška pri učitavanju korisnika';
          this.loading = false;
        },
      });
  }

  blockUser(user: User): void {
    const token = this.authService.getToken();

    if (!token) {
      this.error = 'No token found';
      return;
    }

    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`,
    });

    this.http
      .put(`http://localhost:8081/api/users/${user.id}/block`, {}, { headers })
      .subscribe({
        next: () => {
          user.isBlocked = true;
        },
        error: (err) => {
          console.error('Block error:', err);
          this.error = err.error?.error || 'Greška pri blokiranju';
        },
      });
  }
}
