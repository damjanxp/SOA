import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Tour {
  id?: number;
  name: string;
  description: string;
  authorId?: string;
  difficulty: string;
  tags?: string[];
  status: string;
  price?: number;
  lengthKm?: number;
  createdAt?: string;
  updatedAt?: string;
  publishedAt?: string;
  archivedAt?: string;
  keypoints?: Keypoint[];
}

export interface Keypoint {
  id: number;
  lat: number;
  lon: number;
  name: string;
  description: string;
  imageUrl: string;
  orderIndex: number;
}

export interface Review {
  id: number;
  tourId: number;
  touristId: number;
  rating: number;
  comment: string;
  visitDate: string;
  createdAt: string;
  images: string[];
}

@Injectable({
  providedIn: 'root'
})
export class TourService {
  private apiUrl = `${environment.apiBase}/api/tours`;

  constructor(private http: HttpClient) { }

  // Tour endpoints
  createTour(tour: any): Observable<Tour> {
    return this.http.post<Tour>(this.apiUrl, tour);
  }

  getMyTours(): Observable<Tour[]> {
    return this.http.get<Tour[]>(`${this.apiUrl}/my`);
  }

  getTourById(id: number): Observable<Tour> {
    return this.http.get<Tour>(`${this.apiUrl}/${id}`);
  }

  updateTour(id: number, tour: any): Observable<Tour> {
    return this.http.put<Tour>(`${this.apiUrl}/${id}`, tour);
  }

  deleteTour(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  publishTour(id: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${id}/publish`, {});
  }

  archiveTour(id: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${id}/archive`, {});
  }

  reactivateTour(id: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${id}/reactivate`, {});
  }

  getTransportTimes(tourId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/${tourId}/transport-times`);
  }

  addTransportTime(tourId: number, data: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${tourId}/transport-times`, data);
  }

  deleteTransportTime(tourId: number, ttId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${tourId}/transport-times/${ttId}`);
  }

  addToCart(tourId: number): Observable<any> {
    return this.http.post<any>(`${environment.apiBase}/api/tours/${tourId}/cart`, {});
  }

  // Keypoint endpoints
  addKeypoint(tourId: number, keypoint: any): Observable<Keypoint> {
    return this.http.post<Keypoint>(`${this.apiUrl}/${tourId}/keypoints`, keypoint);
  }

  getKeypoints(tourId: number): Observable<Keypoint[]> {
    return this.http.get<Keypoint[]>(`${this.apiUrl}/${tourId}/keypoints`);
  }

  updateKeypoint(tourId: number, keypointId: number, keypoint: any): Observable<Keypoint> {
    return this.http.put<Keypoint>(`${this.apiUrl}/${tourId}/keypoints/${keypointId}`, keypoint);
  }

  deleteKeypoint(tourId: number, keypointId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${tourId}/keypoints/${keypointId}`);
  }

  // Review endpoints
  createReview(tourId: number, review: any): Observable<Review> {
    return this.http.post<Review>(`${this.apiUrl}/${tourId}/reviews`, review);
  }

  getReviews(tourId: number): Observable<Review[]> {
    return this.http.get<Review[]>(`${this.apiUrl}/${tourId}/reviews`);
  }

  deleteReview(tourId: number, reviewId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${tourId}/reviews/${reviewId}`);
  }

  getPublishedTours(): Observable<Tour[]> {
  return this.http.get<Tour[]>(`${this.apiUrl}/published`);
}
}
