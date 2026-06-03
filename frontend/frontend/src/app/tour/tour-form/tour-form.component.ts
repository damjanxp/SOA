import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { switchMap } from 'rxjs/operators';
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
  transportTypes = ['WALKING', 'BICYCLE', 'CAR'];
  transportTypeLabels: Record<string, string> = { WALKING: '🚶 Peške', BICYCLE: '🚴 Bicikl', CAR: '🚗 Auto' };

  get name() { return this.tourForm.get('name'); }
  get description() { return this.tourForm.get('description'); }
  get lengthKm() { return this.tourForm.get('lengthKm'); }
  get price() { return this.tourForm.get('price'); }

  cancel(): void { this.router.navigate(['/tours']); }

  // Edit mode: loaded from backend
  transportTimes: any[] = [];
  pendingTransportTimes: { transportType: string; durationMinutes: number }[] = [];

  ttType = 'WALKING';
  ttDuration = 30;
  ttError = '';
  ttLoading = false;

  // Inline edit state
  editingTtId: number | null = null;
  editTtType = 'WALKING';
  editTtDuration = 30;

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
        this.loadTransportTimes();
      }
    });
  }

  initForm(): void {
    this.tourForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      difficulty: ['EASY', Validators.required],
      tags: [''],
      lengthKm: [0, [Validators.required, Validators.min(0)]],
      price: [0, [Validators.required, Validators.min(0)]]
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
          lengthKm: data.lengthKm,
          price: data.price || 0
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

  loadTransportTimes(): void {
    if (!this.tourId) return;
    this.tourService.getTransportTimes(this.tourId).subscribe({
      next: (data) => { this.transportTimes = data; },
      error: () => {}
    });
  }

  addTransportTime(): void {
    if (this.ttDuration < 1) {
      this.ttError = 'Trajanje mora biti najmanje 1 minuta';
      return;
    }
    this.ttError = '';

    if (this.isEditMode && this.tourId) {
      this.ttLoading = true;
      this.tourService.addTransportTime(this.tourId, { transportType: this.ttType, durationMinutes: this.ttDuration }).subscribe({
        next: (item) => { this.transportTimes.push(item); this.ttLoading = false; },
        error: (err) => { this.ttError = err?.error?.message || 'Greška'; this.ttLoading = false; }
      });
    } else {
      this.pendingTransportTimes.push({ transportType: this.ttType, durationMinutes: this.ttDuration });
    }
  }

  removePendingTransportTime(index: number): void {
    this.pendingTransportTimes.splice(index, 1);
  }

  deleteTransportTime(id: number): void {
    if (!this.tourId) return;
    this.tourService.deleteTransportTime(this.tourId, id).subscribe({
      next: () => { this.transportTimes = this.transportTimes.filter(t => t.id !== id); },
      error: () => {}
    });
  }

  startEditTransportTime(tt: any): void {
    this.editingTtId = tt.id;
    this.editTtType = tt.transportType;
    this.editTtDuration = tt.durationMinutes;
  }

  cancelEditTransportTime(): void {
    this.editingTtId = null;
  }

  saveEditTransportTime(): void {
    if (!this.tourId || this.editingTtId === null) return;
    if (this.editTtDuration < 1) { this.ttError = 'Trajanje mora biti najmanje 1 minuta'; return; }
    this.ttLoading = true;
    this.ttError = '';
    this.tourService.updateTransportTime(this.tourId, this.editingTtId, {
      transportType: this.editTtType,
      durationMinutes: this.editTtDuration
    }).subscribe({
      next: (updated) => {
        const idx = this.transportTimes.findIndex(t => t.id === this.editingTtId);
        if (idx !== -1) this.transportTimes[idx] = updated;
        this.editingTtId = null;
        this.ttLoading = false;
      },
      error: (err) => { this.ttError = err?.error?.message || 'Greška pri izmjeni'; this.ttLoading = false; }
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
      lengthKm: formValue.lengthKm,
      price: formValue.price
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
      this.tourService.createTour(tourData).pipe(
        switchMap((created: Tour) => {
          if (this.pendingTransportTimes.length === 0) {
            return of({ tour: created });
          }
          const requests = this.pendingTransportTimes.map(tt =>
            this.tourService.addTransportTime(created.id as number, {
              transportType: tt.transportType,
              durationMinutes: tt.durationMinutes
            })
          );
          return forkJoin(requests).pipe(switchMap(() => of({ tour: created })));
        })
      ).subscribe({
        next: ({ tour }) => {
          this.router.navigate(['/tours', tour.id]);
          this.loading = false;
        },
        error: (err) => {
          this.error = err?.error?.message || 'Greška pri kreiranju ture';
          console.error(err);
          this.loading = false;
        }
      });
    }
  }
}

