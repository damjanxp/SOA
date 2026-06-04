import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface TourExecutionResponse {
  id: number;
  touristId: string;
  tourId: number;
  status: string;
  startLat: number;
  startLong: number;
  startedAt: string;
  lastActivityAt: string;
  endedAt?: string;
  completedKeyPoints: CompletedKeyPointResponse[];
}

export interface CompletedKeyPointResponse {
  id: number;
  keyPointId: number;
  completedAt: string;
}

export interface CheckNearbyResponse {
  nearbyFound: boolean;
  keyPointId?: number;
}

@Injectable({ providedIn: 'root' })
export class TourExecutionService {
  private base = `${environment.apiBase}/api/tour-execution`;

  constructor(private http: HttpClient) {}

  startExecution(touristId: string, tourId: number, startLat: number, startLong: number): Observable<TourExecutionResponse> {
    return this.http.post<TourExecutionResponse>(`${this.base}/start`, {
      touristId, tourId, startLat, startLong
    });
  }

  checkNearby(executionId: number, lat: number, lon: number): Observable<CheckNearbyResponse> {
    return this.http.post<CheckNearbyResponse>(`${this.base}/${executionId}/check-nearby`, { lat, lon });
  }

  completeExecution(executionId: number): Observable<TourExecutionResponse> {
    return this.http.post<TourExecutionResponse>(`${this.base}/${executionId}/complete`, {});
  }

  abandonExecution(executionId: number): Observable<TourExecutionResponse> {
    return this.http.post<TourExecutionResponse>(`${this.base}/${executionId}/abandon`, {});
  }

  getActiveExecution(touristId: string, tourId: number): Observable<any> {
  return this.http.get<any>(
    `${environment.apiBase}/api/tour-execution/status?touristId=${touristId}&tourId=${tourId}`
  );
}
}