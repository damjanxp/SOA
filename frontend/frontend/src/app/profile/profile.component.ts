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

  constructor(private profileService: ProfileService) {}

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.profileService.getMyProfile().subscribe({
      next: (res) => {
        this.profile = res.data;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.error || 'Greška pri učitavanju profila';
        this.loading = false;
      },
    });
  }
}
