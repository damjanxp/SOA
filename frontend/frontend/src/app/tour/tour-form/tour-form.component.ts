import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TourService, Tour } from '../tour.service';

@Component({
  selector: 'app-tour-form',
  templateUrl: './tour-form.component.html',
  styleUrls: ['./tour-form.component.scss']
})
export class TourFormComponent implements OnInit {
  tourForm!: FormGroup;
  loading = false;
  error = '';
  isEditMode = false;
  tourId: number | null = null;
  tour: Tour | null = null;

  difficulties = ['EASY', 'MEDIUM', 'HARD'];

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
        this.isEditMode = true;
        this.loadTour();
      }
    });
  }

  initForm(): void {
    this.tourForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      difficulty: ['EASY', Validators.required],
      tags: [''],
      lengthKm: [0, [Validators.required, Validators.min(0)]]
    });
  }

  loadTour(): void {
    if (!this.tourId) return;

    this.loading = true;
    this.tourService.getTourById(this.tourId).subscribe({
      next: (data) => {
        this.tour = data;
        this.tourForm.patchValue({
          name: data.name,
          description: data.description,
          difficulty: data.difficulty,
          tags: data.tags?.join(', ') || '',
          lengthKm: data.lengthKm
        });
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Greška pri učitavanju ture';
        console.error(err);
        this.loading = false;
      }
    });
  }

  onSubmit(): void {
    if (this.tourForm.invalid) {
      this.error = 'Molim popunite sve polje ispravno';
      return;
    }

    this.loading = true;
    const formValue = this.tourForm.value;
    const tags = formValue.tags
      ? formValue.tags.split(',').map((t: string) => t.trim()).filter((t: string) => t)
      : [];

    const tourData = {
      name: formValue.name,
      description: formValue.description,
      difficulty: formValue.difficulty,
      tags,
      lengthKm: formValue.lengthKm
    };

    if (this.isEditMode && this.tourId) {
      this.tourService.updateTour(this.tourId, tourData).subscribe({
        next: () => {
          this.router.navigate(['/tours', this.tourId]);
          this.loading = false;
        },
        error: (err) => {
          this.error = 'Greška pri ažuriranju ture';
          console.error(err);
          this.loading = false;
        }
      });
    } else {
      this.tourService.createTour(tourData).subscribe({
        next: (data) => {
          this.router.navigate(['/tours', data.id]);
          this.loading = false;
        },
        error: (err) => {
          this.error = 'Greška pri kreiranju ture';
          console.error(err);
          this.loading = false;
        }
      });
    }
  }

  cancel(): void {
    if (this.isEditMode && this.tourId) {
      this.router.navigate(['/tours', this.tourId]);
    } else {
      this.router.navigate(['/tours']);
    }
  }

  get name() {
    return this.tourForm.get('name');
  }

  get description() {
    return this.tourForm.get('description');
  }

  get difficulty() {
    return this.tourForm.get('difficulty');
  }

  get lengthKm() {
    return this.tourForm.get('lengthKm');
  }
}
