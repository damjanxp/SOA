import { Component, OnInit, AfterViewInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TourService, Keypoint } from '../tour.service';
import * as L from 'leaflet';

// Fix za Leaflet marker ikone
const iconDefault = L.icon({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

@Component({
  selector: 'app-tour-keypoints',
  templateUrl: './tour-keypoints.component.html',
  styleUrls: ['./tour-keypoints.component.scss']
})
export class TourKeypointsComponent implements OnInit, AfterViewInit {
  private tourMarkers: L.Marker[] = [];
  private tourPolyline: L.Polyline | null = null;
  keypointForm!: FormGroup;
  keypoints: Keypoint[] = [];
  loading = false;
  error = '';
  editingId: number | null = null;
  tourId!: number;

  private map!: L.Map;
  private selectedMarker!: L.Marker;
  selectedLat: number | null = null;
  selectedLon: number | null = null;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private tourService: TourService
  ) {}

  ngOnInit(): void {
  this.initForm();
  this.route.params.subscribe(params => {
    this.tourId = +params['id'];
    this.loadKeypoints();
  });

  // Osvezi mapu kad se promeni orderIndex
  this.keypointForm.get('orderIndex')?.valueChanges.subscribe(() => {
    if (this.editingId) {
      const updated = this.keypoints.map(kp =>
        kp.id === this.editingId
          ? { ...kp, orderIndex: parseInt(this.keypointForm.value.orderIndex) }
          : kp
      );
      this.keypoints = updated;
      this.renderKeypointsOnMap();
    }
  });
}

  ngAfterViewInit(): void {
    this.initMap();
  }

  initMap(): void {
    this.map = L.map('keypoint-map').setView([44.8176, 20.4569], 7);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors'
    }).addTo(this.map);

    this.map.on('click', (e: L.LeafletMouseEvent) => {
      const { lat, lng } = e.latlng;

      if (this.selectedMarker) {
        this.map.removeLayer(this.selectedMarker);
      }

      this.selectedMarker = L.marker([lat, lng], { icon: iconDefault }).addTo(this.map);
      this.selectedLat = lat;
      this.selectedLon = lng;

      this.keypointForm.patchValue({
        lat: parseFloat(lat.toFixed(6)),
        lon: parseFloat(lng.toFixed(6))
      });
    });
  }

  initForm(): void {
    this.keypointForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', Validators.required],
      lat: [0, Validators.required],
      lon: [0, Validators.required],
      imageUrl: [''],
      orderIndex: [0, Validators.required]
    });
  }

  loadKeypoints(): void {
    this.loading = true;
    this.tourService.getKeypoints(this.tourId).subscribe({
      next: (data) => {
        this.keypoints = data;
        this.loading = false;
        this.renderKeypointsOnMap();
      },
      error: () => {
        this.error = 'Greška pri učitavanju keypoints';
        this.loading = false;
      }
    });
  }

 renderKeypointsOnMap(): void {
  if (!this.map) return;

  // Obrisi stare markere i polyline
  this.tourMarkers.forEach(m => this.map.removeLayer(m));
  this.tourMarkers = [];
  if (this.tourPolyline) {
    this.map.removeLayer(this.tourPolyline);
    this.tourPolyline = null;
  }

  const sorted = [...this.keypoints].sort((a, b) => a.orderIndex - b.orderIndex);

  sorted.forEach(kp => {
    const marker = L.marker([kp.lat, kp.lon], { icon: iconDefault })
      .addTo(this.map)
      .bindPopup(`<b>${kp.orderIndex}. ${kp.name}</b><br>${kp.description}`);
    this.tourMarkers.push(marker);
  });

  if (sorted.length > 1) {
    const latLngs = sorted.map(kp => L.latLng(kp.lat, kp.lon));
    this.tourPolyline = L.polyline(latLngs, {
      color: '#3388ff',
      weight: 3,
      opacity: 0.7
    }).addTo(this.map);

    const group = L.featureGroup(this.tourMarkers);
    this.map.fitBounds(group.getBounds().pad(0.1));
  }
}

  onSubmit(): void {
    if (this.keypointForm.invalid) return;
    const data = this.keypointForm.value;

    if (this.editingId) {
      this.tourService.updateKeypoint(this.tourId, this.editingId, data)
        .subscribe(() => { this.reset(); this.loadKeypoints(); });
    } else {
      this.tourService.addKeypoint(this.tourId, data)
        .subscribe(() => { this.reset(); this.loadKeypoints(); });
    }
  }

  editKeypoint(kp: Keypoint): void {
    this.editingId = kp.id;
    this.keypointForm.patchValue({
      name: kp.name,
      description: kp.description,
      lat: kp.lat,
      lon: kp.lon,
      imageUrl: kp.imageUrl,
      orderIndex: kp.orderIndex
    });

    if (this.selectedMarker) this.map.removeLayer(this.selectedMarker);
    this.selectedMarker = L.marker([kp.lat, kp.lon], { icon: iconDefault }).addTo(this.map);
    this.map.setView([kp.lat, kp.lon], 12);
  }

  deleteKeypoint(id: number): void {
    this.tourService.deleteKeypoint(this.tourId, id)
      .subscribe(() => this.loadKeypoints());
  }

  cancelEdit(): void { this.reset(); }

  reset(): void {
    this.editingId = null;
    this.keypointForm.reset({ lat: 0, lon: 0, orderIndex: 0 });
    if (this.selectedMarker) this.map.removeLayer(this.selectedMarker);
  }

  back(): void {
    this.router.navigate(['/tours', this.tourId]);
  }
}