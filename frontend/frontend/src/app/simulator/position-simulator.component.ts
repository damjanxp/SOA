import { Component, OnInit, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PositionService, Position } from '../services/position.service';
import * as L from 'leaflet';
import { Subscription } from 'rxjs';

const icon = L.icon({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
});

@Component({
  selector: 'app-position-simulator',
  templateUrl: './position-simulator.component.html',
  styleUrls: ['./position-simulator.component.scss']
})
export class PositionSimulatorComponent implements OnInit, AfterViewInit, OnDestroy {
  currentPosition: Position | null = null;
  private map!: L.Map;
  private marker: L.Marker | null = null;
  private sub!: Subscription;

  constructor(private positionService: PositionService) {}

  ngOnInit(): void {
    this.sub = this.positionService.currentLocation$.subscribe(pos => {
      this.currentPosition = pos;
    });
  }

  ngAfterViewInit(): void {
    const saved = this.positionService.getCurrentLocation();
    const center: L.LatLngTuple = saved ? [saved.lat, saved.lng] : [44.8176, 20.4569];

    this.map = L.map('simulator-map').setView(center, 13);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors'
    }).addTo(this.map);

    if (saved) {
      this.placeMarker(saved.lat, saved.lng);
    }

    this.map.on('click', (e: L.LeafletMouseEvent) => {
      const { lat, lng } = e.latlng;
      this.positionService.setCurrentLocation({ lat, lng });
      this.placeMarker(lat, lng);
    });
  }

  private placeMarker(lat: number, lng: number): void {
    if (this.marker) {
      this.map.removeLayer(this.marker);
    }
    this.marker = L.marker([lat, lng], { icon })
      .addTo(this.map)
      .bindPopup(`📍 ${lat.toFixed(6)}, ${lng.toFixed(6)}`)
      .openPopup();
  }

  clearPosition(): void {
    this.positionService.clearLocation();
    if (this.marker) {
      this.map.removeLayer(this.marker);
      this.marker = null;
    }
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    if (this.map) this.map.remove();
  }
}
