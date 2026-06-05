import { Component, OnInit, AfterViewInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TourExecutionService } from '../tour-execution.service';
import { TourService, Keypoint } from '../tour.service';
import { PositionService } from '../../services/position.service';
import { AuthService } from '../../auth/auth.service';
import * as L from 'leaflet';

const iconDefault = L.icon({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowSize: [41, 41]
});

const iconGreen = L.icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-green.png',
  iconRetinaUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowSize: [41, 41]
});

const iconRed = L.icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-red.png',
  iconRetinaUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowSize: [41, 41]
});

@Component({
  selector: 'app-tour-execution',
  templateUrl: './tour-execution.component.html',
  styleUrls: ['./tour-execution.component.scss']
})
export class TourExecutionComponent implements OnInit, AfterViewInit, OnDestroy {
  tourId!: number;
  executionId: number | null = null;
  keypoints: Keypoint[] = [];
  completedKeyPointIds: Set<number> = new Set();
  toastMessage = '';
  error = '';
  loading = true;
  status = 'ACTIVE';

  private map!: L.Map;
  private touristMarker: L.Marker | null = null;
  private keypointMarkers: Map<number, L.Marker> = new Map();
  private intervalId: any = null;

  constructor(
    private route: ActivatedRoute,
    public router: Router,
    private executionService: TourExecutionService,
    private tourService: TourService,
    private positionService: PositionService,
    private authService: AuthService
  ) {}

  get allKeypointsCompleted(): boolean {
    return this.keypoints.length > 0 && this.completedKeyPointIds.size === this.keypoints.length;
  }

  ngOnInit(): void {
  this.tourId = +this.route.snapshot.params['tourId'];
  const position = this.positionService.getCurrentLocation();

  if (!position) {
    this.error = 'Postavite lokaciju u simulatoru pre pokretanja ture.';
    this.loading = false;
    return;
  }

  const touristId = this.authService.getUserId();

  this.tourService.getKeypoints(this.tourId).subscribe({
    next: (kps) => {
      this.keypoints = kps;
      this.executionService.startExecution(touristId, this.tourId, position.lat, position.lng).subscribe({
        next: (exec) => {
          this.executionId = exec.id;
          this.status = exec.status;

          // Povuci pun status sa completedKeyPoints
          this.executionService.getLatestExecution(touristId, this.tourId).subscribe({
            next: (fullExec) => {
              if (fullExec && fullExec.completedKeyPoints && fullExec.completedKeyPoints.length > 0) {
                fullExec.completedKeyPoints.forEach((c: any) => {
                  this.completedKeyPointIds.add(c.keyPointId);
                });
              }
              this.loading = false;
              this.tryInitMap();
              if (this.status === 'ACTIVE') {
                this.startInterval();
              }
            },
            error: () => {
              // Ako status poziv ne uspe, nastavi bez completedKeyPoints
              this.loading = false;
              this.tryInitMap();
              if (this.status === 'ACTIVE') {
                this.startInterval();
              }
            }
          });
        },
        error: (err) => {
          this.error = err?.error?.message || err?.error?.error || 'Greška pri pokretanju ture.';
          this.loading = false;
        }
      });
    },
    error: () => {
      this.error = 'Greška pri učitavanju ključnih tačaka.';
      this.loading = false;
    }
  });
}

  ngAfterViewInit(): void {
    // mapa se inicijalizuje tek nakon što loading postane false i DOM se ažurira
  }

  private tryInitMap(): void {
    setTimeout(() => {
      const mapEl = document.getElementById('execution-map');
      if (!mapEl || this.map) return;
      this.initMap();
    }, 500);
  }

  initMap(): void {
    const position = this.positionService.getCurrentLocation();
    const center: L.LatLngTuple = position ? [position.lat, position.lng] : [44.8176, 20.4569];

    this.map = L.map('execution-map').setView(center, 14);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors'
    }).addTo(this.map);

    if (position) {
      this.touristMarker = L.marker([position.lat, position.lng], { icon: iconRed })
        .addTo(this.map)
        .bindPopup('Vi ste ovde');
    }

    this.keypoints.forEach(kp => {
      const marker = L.marker([kp.lat, kp.lon], { icon: iconDefault })
        .addTo(this.map)
        .bindPopup(`<b>${kp.name}</b><br>${kp.description}`);
      this.keypointMarkers.set(kp.id, marker);
    });

    this.completedKeyPointIds.forEach(id => {
      const marker = this.keypointMarkers.get(id);
      if (marker) marker.setIcon(iconGreen);
    });
  }

  startInterval(): void {
    this.intervalId = setInterval(() => {
      if (!this.executionId) return;
      const pos = this.positionService.getCurrentLocation();
      if (!pos) return;

      this.updateTouristMarker(pos.lat, pos.lng);

      this.executionService.checkNearby(this.executionId, pos.lat, pos.lng).subscribe({
        next: (res) => {
          if (res.nearbyFound && res.keyPointId && !this.completedKeyPointIds.has(res.keyPointId)) {
            this.completedKeyPointIds.add(res.keyPointId);
            const kp = this.keypoints.find(k => k.id === res.keyPointId);
            this.showToast(`Stigli ste do: ${kp?.name || 'ključne tačke'}`);
            const marker = this.keypointMarkers.get(res.keyPointId);
            if (marker) marker.setIcon(iconGreen);

            if (this.allKeypointsCompleted) {
              this.stopInterval();
              this.autoComplete();
            }
          }
        },
        error: () => {}
      });
    }, 10000);
  }

  autoComplete(): void {
    if (!this.executionId) return;
    this.executionService.completeExecution(this.executionId).subscribe({
      next: () => {
        this.status = 'COMPLETED';
        this.showToast('Čestitamo! Obišli ste sve tačke — tura završena!');
        setTimeout(() => this.router.navigate(['/my-purchases']), 3000);
      },
      error: () => {}
    });
  }

  updateTouristMarker(lat: number, lng: number): void {
    if (!this.map) return;
    if (this.touristMarker) this.map.removeLayer(this.touristMarker);
    this.touristMarker = L.marker([lat, lng], { icon: iconRed })
      .addTo(this.map).bindPopup('Vi ste ovde');
  }

  showToast(msg: string): void {
    this.toastMessage = msg;
    setTimeout(() => this.toastMessage = '', 4000);
  }

  completeTour(): void {
    if (!this.executionId || !this.allKeypointsCompleted) return;
    this.executionService.completeExecution(this.executionId).subscribe({
      next: () => {
        this.stopInterval();
        this.status = 'COMPLETED';
        this.showToast('Tura završena!');
        setTimeout(() => this.router.navigate(['/my-purchases']), 2000);
      },
      error: (err) => { this.error = err?.error?.message || 'Greška pri završavanju ture.'; }
    });
  }

  abandonTour(): void {
    if (!this.executionId) return;
    this.executionService.abandonExecution(this.executionId).subscribe({
      next: () => {
        this.stopInterval();
        this.status = 'ABANDONED';
        this.showToast('Napustili ste turu.');
        setTimeout(() => this.router.navigate(['/my-purchases']), 2000);
      },
      error: (err) => { this.error = err?.error?.message || 'Greška pri napuštanju ture.'; }
    });
  }

  stopInterval(): void {
    if (this.intervalId) { clearInterval(this.intervalId); this.intervalId = null; }
  }

  ngOnDestroy(): void {
    this.stopInterval();
    if (this.map) this.map.remove();
  }
}