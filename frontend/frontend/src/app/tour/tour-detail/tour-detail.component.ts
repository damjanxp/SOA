import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TourService, Tour, Keypoint, Review } from '../tour.service';

@Component({
  selector: 'app-tour-detail',
  templateUrl: './tour-detail.component.html',
  styleUrls: ['./tour-detail.component.scss']
})
export class TourDetailComponent implements OnInit {
  tour: Tour | null = null;
  keypoints: Keypoint[] = [];
  reviews: Review[] = [];

  loading = false;
  error = '';

  activeTab: 'keypoints' | 'reviews' = 'keypoints';

  constructor(
    private tourService: TourService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      const id = params['id'];
      if (id) {
        this.loadTour(+id);
        this.loadReviews(+id);
      }
    });
  }

  loadTour(id: number): void {
    this.loading = true;
    this.error = '';
    this.tourService.getTourById(id).subscribe({
      next: (data) => {
        this.tour = data;
        this.keypoints = data.keypoints ?? [];
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.error = 'Greška pri učitavanju ture';
        this.loading = false;
      }
    });
  }

  loadReviews(id: number): void {
    this.tourService.getReviews(id).subscribe({
      next: (data) => this.reviews = data,
      error: (err) => console.error(err)
    });
  }

  getRatingStars(rating: number): string[] {
    return Array.from({ length: 5 }, (_, i) => i < rating ? '★' : '☆');
  }

  setActiveTab(tab: 'keypoints' | 'reviews'): void {
    this.activeTab = tab;
  }

  editTour(): void {
    if (this.tour?.id) this.router.navigate(['/tours', this.tour.id, 'edit']);
  }

  manageKeypoints(): void {
    if (this.tour?.id) this.router.navigate(['/tours', this.tour.id, 'keypoints']);
  }

  back(): void {
    this.router.navigate(['/tours']);
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'DRAFT': return 'draft';
      case 'PUBLISHED': return 'published';
      case 'ARCHIVED': return 'archived';
      default: return '';
    }
  }
}