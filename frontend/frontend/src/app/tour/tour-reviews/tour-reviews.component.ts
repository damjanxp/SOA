import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TourService, Review } from '../tour.service';

@Component({
  selector: 'app-tour-reviews',
  templateUrl: './tour-reviews.component.html',
  styleUrls: ['./tour-reviews.component.scss']
})
export class TourReviewsComponent implements OnInit {
  tourId: number | null = null;
  reviews: Review[] = [];
  reviewForm!: FormGroup;
  loading = false;
  error = '';
  success = '';

  constructor(
    private fb: FormBuilder,
    private tourService: TourService,
    private route: ActivatedRoute,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.initForm();
    
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.tourId = +params['id'];
        this.loadReviews();
      }
    });
  }

  initForm(): void {
    this.reviewForm = this.fb.group({
      rating: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
      comment: ['', Validators.required],
      visitDate: ['', Validators.required],
      images: ['']
    });
  }

  loadReviews(): void {
    if (!this.tourId) return;

    this.loading = true;
    this.tourService.getReviews(this.tourId).subscribe({
      next: (data) => {
        this.reviews = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Greška pri učitavanju recenzija';
        console.error(err);
        this.loading = false;
      }
    });
  }

  onSubmit(): void {
    if (this.reviewForm.invalid || !this.tourId) {
      this.error = 'Molim popunite sve polje ispravno';
      return;
    }

    const formValue = this.reviewForm.value;
    const images = formValue.images
      ? formValue.images.split(',').map((img: string) => img.trim()).filter((img: string) => img)
      : [];

    const reviewData = {
      rating: parseInt(formValue.rating),
      comment: formValue.comment,
      visitDate: formValue.visitDate,
      images: images
    };

    this.tourService.createReview(this.tourId, reviewData).subscribe({
      next: () => {
        this.success = 'Recenzija je uspešno ostavljena!';
        this.reviewForm.reset({ rating: 5 });
        this.loadReviews();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.error = 'Greška pri slanju recenzije';
        console.error(err);
      }
    });
  }

  deleteReview(id: number): void {
    if (confirm('Da li ste sigurni?') && this.tourId) {
      this.tourService.deleteReview(this.tourId, id).subscribe({
        next: () => {
          this.loadReviews();
          this.success = 'Recenzija je obrisana';
          setTimeout(() => this.success = '', 3000);
        },
        error: (err) => {
          this.error = 'Greška pri brisanju recenzije';
          console.error(err);
        }
      });
    }
  }

  getRatingStars(rating: number): string[] {
    const stars: string[] = [];
    for (let i = 1; i <= 5; i++) {
      stars.push(i <= rating ? '★' : '☆');
    }
    return stars;
  }

  back(): void {
    if (this.tourId) {
      this.router.navigate(['/tours', this.tourId]);
    } else {
      this.router.navigate(['/tours']);
    }
  }
}
