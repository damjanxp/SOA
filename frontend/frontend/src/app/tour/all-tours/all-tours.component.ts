import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { TourService, Tour } from '../tour.service';

@Component({
  selector: 'app-all-tours',
  templateUrl: './all-tours.component.html',
  styleUrls: ['./all-tours.component.scss']
})
export class AllToursComponent implements OnInit {
  tours: Tour[] = [];
  loading = false;
  error = '';

  constructor(private tourService: TourService, private router: Router) {}

  ngOnInit(): void {
    this.loadTours();
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
}
