import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { TourService } from '../tour.service';
import { TourExecutionService } from '../tour-execution.service';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-my-purchases',
  templateUrl: './my-purchases.component.html',
  styleUrls: ['./my-purchases.component.scss']
})
export class MyPurchasesComponent implements OnInit {
  purchases: any[] = [];
  executionStatusMap: { [tourId: number]: string } = {};
  loading = false;
  error = '';

  constructor(
    private tourService: TourService,
    private executionService: TourExecutionService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const userId = this.authService.getUserId();
    if (!userId) return;
    this.loading = true;
    this.tourService.getPurchases(userId).subscribe({
      next: (data) => {
        this.purchases = data;
        this.loading = false;
        this.loadExecutionStatuses(userId);
      },
      error: () => { this.error = 'Greška pri učitavanju kupljenih tura'; this.loading = false; }
    });
  }

  loadExecutionStatuses(userId: string): void {
    this.purchases.forEach(p => {
      this.executionService.getActiveExecution(userId, p.tourId).subscribe({
        next: (exec) => {
          if (exec) this.executionStatusMap[p.tourId] = exec.status;
        },
        error: () => {}
      });
    });
  }

  getButtonLabel(tourId: number): string {
    const status = this.executionStatusMap[tourId];
    if (status === 'ACTIVE') return '▶ Nastavi';
    if (status === 'COMPLETED') return '✅ Završena';
    return '▶ Pokreni';
  }

  isStartDisabled(tourId: number): boolean {
    return this.executionStatusMap[tourId] === 'COMPLETED';
  }

  startOrContinue(tourId: number): void {
    this.router.navigate(['/tour-execution', tourId]);
  }

  viewTour(tourId: number): void {
    this.router.navigate(['/tours', tourId]);
  }
}