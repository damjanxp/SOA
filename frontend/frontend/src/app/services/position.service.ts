import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface Position {
  lat: number;
  lng: number;
}

@Injectable({
  providedIn: 'root'
})
export class PositionService {
  private readonly STORAGE_KEY = 'tourist_position';

  currentLocation$ = new BehaviorSubject<Position | null>(this.loadFromStorage());

  private loadFromStorage(): Position | null {
    const saved = localStorage.getItem(this.STORAGE_KEY);
    return saved ? JSON.parse(saved) : null;
  }

  setCurrentLocation(pos: Position): void {
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(pos));
    this.currentLocation$.next(pos);
  }

  getCurrentLocation(): Position | null {
    return this.currentLocation$.getValue();
  }

  clearLocation(): void {
    localStorage.removeItem(this.STORAGE_KEY);
    this.currentLocation$.next(null);
  }
}
