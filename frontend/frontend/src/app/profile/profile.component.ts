import { Component, OnInit } from '@angular/core';
import { ProfileService } from '../profile.service';
import { FollowerService, UserEntry, Recommendation } from '../services/follower.service';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss'],
})
export class ProfileComponent implements OnInit {
  profile: any;
  loading = true;
  error = '';
  success = '';

  isEditing = false;
  form = {
    firstName: '',
    lastName: '',
    profileImageUrl: '',
    bio: '',
    motto: ''
  };

  // Social
  currentUserId = '';
  following: UserEntry[] = [];
  followers: UserEntry[] = [];
  recommendations: Recommendation[] = [];
  showFollowing = false;
  showFollowers = false;

  constructor(
    private profileService: ProfileService,
    private followerService: FollowerService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.currentUserId = this.authService.getUserId();
    this.loadProfile();
  }

  loadProfile(): void {
    this.profileService.getMyProfile().subscribe({
      next: (res) => {
        this.profile = res.data;
        this.form.firstName = this.profile?.firstName || '';
        this.form.lastName = this.profile?.lastName || '';
        this.form.profileImageUrl = this.profile?.profileImageUrl || '';
        this.form.bio = this.profile?.bio || '';
        this.form.motto = this.profile?.motto || '';
        this.loading = false;
        this.loadSocialData();
      },
      error: (err) => {
        this.error = err.error?.error || 'Greška pri učitavanju profila';
        this.loading = false;
      },
    });
  }

  loadSocialData(): void {
    if (!this.currentUserId) return;

    this.followerService.getFollowing(this.currentUserId).subscribe({
      next: (list) => {
        this.following = list || [];
        // Resolve missing usernames
        this.following.forEach((u, i) => {
          if (!u.username) {
            this.followerService.getUserInfo(u.userId).subscribe({
              next: (info) => { this.following[i] = { ...u, username: info.username }; },
              error: () => {}
            });
          }
        });
      },
      error: () => { this.following = []; }
    });

    this.followerService.getFollowers(this.currentUserId).subscribe({
      next: (list) => {
        this.followers = list || [];
        // Resolve missing usernames
        this.followers.forEach((u, i) => {
          if (!u.username) {
            this.followerService.getUserInfo(u.userId).subscribe({
              next: (info) => { this.followers[i] = { ...u, username: info.username }; },
              error: () => {}
            });
          }
        });
      },
      error: () => { this.followers = []; }
    });

    this.followerService.getRecommendations(this.currentUserId).subscribe({
      next: (list) => { this.recommendations = list || []; },
      error: () => { this.recommendations = []; }
    });
  }

  toggleFollowingList(): void {
    this.showFollowing = !this.showFollowing;
    this.showFollowers = false;
  }

  toggleFollowersList(): void {
    this.showFollowers = !this.showFollowers;
    this.showFollowing = false;
  }

  unfollow(userId: string): void {
    this.followerService.unfollow(userId).subscribe({
      next: () => {
        this.following = this.following.filter(u => u.userId !== userId);
        // Also refresh followers of this user in case
        this.followerService.getFollowers(userId).subscribe();
      },
      error: () => {}
    });
  }

  follow(userId: string, username: string): void {
    this.followerService.follow(userId).subscribe({
      next: () => {
        this.recommendations = this.recommendations.filter(r => r.userId !== userId);
        this.following.push({ userId, username });
      },
      error: () => {}
    });
  }

  startEdit(): void {
    this.success = '';
    this.error = '';
    this.isEditing = true;
  }

  cancelEdit(): void {
    this.isEditing = false;
    this.form.firstName = this.profile?.firstName || '';
    this.form.lastName = this.profile?.lastName || '';
    this.form.profileImageUrl = this.profile?.profileImageUrl || '';
    this.form.bio = this.profile?.bio || '';
    this.form.motto = this.profile?.motto || '';
  }

  saveProfile(): void {
    this.error = '';
    this.success = '';

    this.profileService.updateProfile({
      firstName: this.form.firstName,
      lastName: this.form.lastName,
      profileImageUrl: this.form.profileImageUrl,
      bio: this.form.bio,
      motto: this.form.motto
    }).subscribe({
      next: (res) => {
        this.profile = res.data || res;
        this.isEditing = false;
        this.success = 'Profil uspešno izmenjen.';
      },
      error: (err) => {
        this.error = err.error?.error || 'Greška pri izmeni profila';
      }
    });
  }
}
