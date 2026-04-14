import { Component, OnInit } from '@angular/core';
import { ProfileService } from '../profile.service';

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

  constructor(private profileService: ProfileService) {}

  ngOnInit(): void {
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
      },
      error: (err) => {
        this.error = err.error?.error || 'Greška pri učitavanju profila';
        this.loading = false;
      },
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
