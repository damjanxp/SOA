import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthService } from './auth/auth.service';
import { environment } from '../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class ProfileService {
  constructor(
    private http: HttpClient,
    private authService: AuthService,
  ) {}

  getMyProfile() {
    return this.http.get<any>(`${environment.apiBase}/api/profile/me`);
  }

  updateProfile(data: {
    firstName?: string;
    lastName?: string;
    profileImageUrl?: string;
    bio?: string;
    motto?: string;
  }) {
    const userId = this.authService.getUserId();
    return this.http.put<any>(`${environment.apiBase}/api/users/${userId}/profile`, data);
  }
}
