import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { TourService, Tour } from '../tour.service';

@Component({
  selector: 'app-tour-list',
  templateUrl: './tour-list.component.html',
  styleUrls: ['./tour-list.component.scss']
})
export class TourListComponent implements OnInit {
  tours: Tour[] = [];
  loading = false;
  error = '';
  tourErrors: { [id: number]: string } = {};
  tourSuccess: { [id: number]: string } = {};

  constructor(private tourService: TourService, private router: Router) { }

  ngOnInit(): void {
    this.loadMyTours();
  }

  loadMyTours(): void {
    this.loading = true;
    this.error = '';
    this.tourService.getMyTours().subscribe({
      next: (data) => {
        this.tours = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Greška pri učitavanju tura';
        console.error(err);
        this.loading = false;
      }
    });
  }

  createNewTour(): void {
    this.router.navigate(['/tours/new']);
  }

  viewTour(id: number): void {
    this.router.navigate(['/tours', id]);
  }

  editTour(id: number): void {
    this.router.navigate(['/tours', id, 'edit']);
  }

  deleteTour(id: number): void {
    if (confirm('Da li ste sigurni da želite da obrišete ovu turu?')) {
      this.tourService.deleteTour(id).subscribe({
        next: () => {
          this.loadMyTours();
        },
        error: (err) => {
          console.error('Greška pri brisanju ture', err);
          this.error = 'Greška pri brisanju ture';
        }
      });
    }
  }

  publishTour(id: number): void {
    this.tourErrors[id] = '';
    this.tourSuccess[id] = '';
    this.tourService.publishTour(id).subscribe({
      next: (res) => {
        this.tourSuccess[id] = 'Tura uspješno objavljena!';
        const tour = this.tours.find(t => t.id === id);
        if (tour) { tour.status = 'PUBLISHED'; (tour as any).publishedAt = res.publishedAt || new Date().toISOString(); }
      },
      error: (err) => {
        this.tourErrors[id] = err?.error?.message || err?.error?.error || 'Greška pri objavljivanju ture';
      }
    });
  }

  archiveTour(id: number): void {
    this.tourErrors[id] = '';
    this.tourSuccess[id] = '';
    this.tourService.archiveTour(id).subscribe({
      next: () => {
        this.tourSuccess[id] = 'Tura arhivirana.';
        const tour = this.tours.find(t => t.id === id);
        if (tour) tour.status = 'ARCHIVED';
      },
      error: (err) => {
        this.tourErrors[id] = err?.error?.message || err?.error?.error || 'Greška pri arhiviranju';
      }
    });
  }

  reactivateTour(id: number): void {
    this.tourErrors[id] = '';
    this.tourSuccess[id] = '';
    this.tourService.reactivateTour(id).subscribe({
      next: () => {
        this.tourSuccess[id] = 'Tura reaktivirana.';
        const tour = this.tours.find(t => t.id === id);
        if (tour) tour.status = 'PUBLISHED';
      },
      error: (err) => {
        this.tourErrors[id] = err?.error?.message || err?.error?.error || 'Greška pri reaktivaciji';
      }
    });
  }

  getStatusBadge(status: string): string {
    switch (status) {
      case 'DRAFT':
        return 'draft';
      case 'PUBLISHED':
        return 'published';
      case 'ARCHIVED':
        return 'archived';
      default:
        return '';
    }
  }
}
