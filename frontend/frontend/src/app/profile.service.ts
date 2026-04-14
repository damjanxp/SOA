import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from './auth/auth.service';

@Injectable({
  providedIn: 'root',
})
export class ProfileService {
  constructor(
    private http: HttpClient,
    private authService: AuthService,
  ) {}

  getMyProfile() {
    const token = this.authService.getToken();

    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`,
    });

    return this.http.get<any>('http://localhost:8081/api/profile/me', {
      headers,
    });
  }

  updateProfile(data: {
    firstName?: string;
    lastName?: string;
    profileImageUrl?: string;
    bio?: string;
    motto?: string;
  }) {
    const token = this.authService.getToken();
    const userId = this.getUserIdFromToken(token);

    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`,
    });

    return this.http.put<any>(`http://localhost:8081/api/users/${userId}/profile`, data, {
      headers,
    });
  }

  private getUserIdFromToken(token: string | null): string {
    if (!token) {
      return '';
    }
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload?.userId || '';
    } catch {
      return '';
    }
  }
}
