import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { TourService, Tour } from '../tour.service';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-all-tours',
  templateUrl: './all-tours.component.html',
  styleUrls: ['./all-tours.component.scss']
})
export class AllToursComponent implements OnInit {
  tours: Tour[] = [];
  loading = false;
  error = '';
  cartMessages: { [id: number]: string } = {};
  cartErrors: { [id: number]: string } = {};
  cartTourIds: Set<number> = new Set();
  purchasedTourIds: Set<number> = new Set();

  constructor(
    private tourService: TourService,
    private router: Router,
    private authService: AuthService
  ) {}

  get isTourist(): boolean {
    return this.authService.getRole() === 'tourist';
  }

  ngOnInit(): void {
    this.loadTours();
    this.loadCartAndPurchases();
  }

  loadCartAndPurchases(): void {
    const userId = this.authService.getUserId();
    if (!userId) return;
    this.tourService.getCart(userId).subscribe({
      next: (cart) => { this.cartTourIds = new Set(cart.items.map(i => i.tourId)); },
      error: () => {}
    });
    this.tourService.getPurchases(userId).subscribe({
      next: (purchases) => { this.purchasedTourIds = new Set(purchases.map((p: any) => p.tourId)); },
      error: () => {}
    });
  }

  loadTours(): void {
    this.loading = true;
    this.tourService.getPublishedTours().subscribe({
      next: (data) => {
        this.tours = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Greška pri učitavanju tura';
        this.loading = false;
      }
    });
  }

  viewTour(id: number): void {
    this.router.navigate(['/tours', id]);
  }

  addToCart(tourId: number): void {
    const userId = this.authService.getUserId();
    if (!userId) return;
    this.cartMessages[tourId] = '';
    this.cartErrors[tourId] = '';
    this.tourService.addToCart(userId, tourId).subscribe({
      next: () => { this.cartTourIds.add(tourId); },
      error: (err) => { this.cartErrors[tourId] = err?.error?.message || 'Greška pri dodavanju u korpu'; }
    });
  }

  startTour(id: number): void {
  this.router.navigate(['/tour-execution', id]);
}

}
