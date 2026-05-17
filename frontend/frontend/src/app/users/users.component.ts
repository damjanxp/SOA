import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../auth/auth.service';
import { FollowerService, Recommendation } from '../services/follower.service';
import { environment } from '../../environments/environment';

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

  followingMap: { [userId: string]: boolean } = {};
  recommendations: Recommendation[] = [];
  myUserId = '';

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private followerService: FollowerService,
  ) {}

  ngOnInit(): void {
    this.myUserId = this.authService.getUserId();
    this.loadUsers();
    this.loadRecommendations();
  }

  loadUsers(): void {
    this.http
      .get<any>(`${environment.apiBase}/api/users`)
      .subscribe({
        next: (res) => {
          const data = res.data || res;
          this.users = Array.isArray(data) ? data : [];
          this.loading = false;
          this.checkFollowingStatuses();
        },
        error: (err) => {
          this.error = err.error?.error || 'Greška pri učitavanju korisnika';
          this.loading = false;
        },
      });
  }

  checkFollowingStatuses(): void {
    this.users.forEach((user) => {
      if (user.id === this.myUserId) return;
      this.followerService.isFollowing(user.id).subscribe({
        next: (res) => {
          this.followingMap[user.id] = res.isFollowing;
        },
        error: () => {
          this.followingMap[user.id] = false;
        },
      });
    });
  }

  followUser(userId: string): void {
    this.followerService.follow(userId).subscribe({
      next: () => {
        this.followingMap[userId] = true;
      },
      error: (err) => {
        this.error = err.error?.error || 'Greška pri praćenju korisnika';
      },
    });
  }

  unfollowUser(userId: string): void {
    this.followerService.unfollow(userId).subscribe({
      next: () => {
        this.followingMap[userId] = false;
      },
      error: (err) => {
        this.error = err.error?.error || 'Greška pri otpraćivanju korisnika';
      },
    });
  }

  loadRecommendations(): void {
    if (!this.myUserId) return;
    this.followerService.getRecommendations(this.myUserId).subscribe({
      next: (recs) => {
        this.recommendations = recs;
      },
      error: () => {
        // Silently ignore — recommendations are optional
      },
    });
  }

  blockUser(user: User): void {
    this.http
      .put(`${environment.apiBase}/api/users/${user.id}/block`, {})
      .subscribe({
        next: () => {
          user.isBlocked = true;
        },
        error: (err) => {
          this.error = err.error?.error || 'Greška pri blokiranju';
        },
      });
  }
}

