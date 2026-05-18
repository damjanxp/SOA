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
    this.tourService.publishTour(id).subscribe({
      next: () => {
        this.loadMyTours();
      },
      error: (err) => {
        console.error('Greška pri objavljivanju ture', err);
        this.error = 'Greška pri objavljivanju ture';
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
