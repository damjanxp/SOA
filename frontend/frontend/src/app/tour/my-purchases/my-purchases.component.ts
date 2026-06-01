import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { TourService } from '../tour.service';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-my-purchases',
  templateUrl: './my-purchases.component.html',
  styleUrls: ['./my-purchases.component.scss']
})
export class MyPurchasesComponent implements OnInit {
  purchases: any[] = [];
  loading = false;
  error = '';

  constructor(
    private tourService: TourService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const userId = this.authService.getUserId();
    if (!userId) return;
    this.loading = true;
    this.tourService.getPurchases(userId).subscribe({
      next: (data) => { this.purchases = data; this.loading = false; },
      error: () => { this.error = 'Greška pri učitavanju kupljenih tura'; this.loading = false; }
    });
  }

  viewTour(tourId: number): void {
    this.router.navigate(['/tours', tourId]);
  }
}
