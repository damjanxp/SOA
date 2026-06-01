import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { TourService, CartResponse } from '../tour.service';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-cart',
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.scss']
})
export class CartComponent implements OnInit {
  cart: CartResponse | null = null;
  loading = false;
  error = '';
  checkoutMessage = '';
  checkoutError = '';
  checkoutLoading = false;
  checkoutTokens: { tourId: number; token: string }[] = [];

  constructor(
    private tourService: TourService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadCart();
  }

  loadCart(): void {
    const userId = this.authService.getUserId();
    if (!userId) return;
    this.loading = true;
    this.tourService.getCart(userId).subscribe({
      next: (data) => { this.cart = data; this.loading = false; },
      error: (err) => {
        this.error = err?.error?.message || 'Greška pri učitavanju korpe';
        this.loading = false;
      }
    });
  }

  removeItem(tourId: number): void {
    const userId = this.authService.getUserId();
    if (!userId) return;
    this.tourService.removeFromCart(userId, tourId).subscribe({
      next: (data) => { this.cart = data; },
      error: (err) => { this.error = err?.error?.message || 'Greška pri uklanjanju ture'; }
    });
  }

  checkout(): void {
    const userId = this.authService.getUserId();
    if (!userId) return;
    this.checkoutLoading = true;
    this.checkoutMessage = '';
    this.checkoutError = '';
    this.tourService.checkout(userId).subscribe({
      next: (tokens: any[]) => {
        this.checkoutMessage = 'Kupovina uspješna! Ture su sada dostupne.';
        this.checkoutTokens = tokens.map((t: any) => ({ tourId: t.tourId, token: t.token }));
        this.checkoutLoading = false;
        this.loadCart();
      },
      error: (err) => {
        this.checkoutError = err?.error?.error || err?.error?.message || 'Greška pri kupovini';
        this.checkoutLoading = false;
      }
    });
  }
}

