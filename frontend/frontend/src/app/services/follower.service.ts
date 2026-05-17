import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Recommendation {
  userId: string;
  username: string;
  mutualCount: number;
}

@Injectable({
  providedIn: 'root',
})
export class FollowerService {
  private readonly apiBase = environment.apiBase;

  constructor(private http: HttpClient) {}

  follow(userId: string): Observable<any> {
    return this.http.post(`${this.apiBase}/api/followers/follow/${userId}`, {});
  }

  unfollow(userId: string): Observable<any> {
    return this.http.delete(`${this.apiBase}/api/followers/unfollow/${userId}`);
  }

  isFollowing(userId: string): Observable<{ isFollowing: boolean }> {
    return this.http.get<{ isFollowing: boolean }>(
      `${this.apiBase}/api/followers/is-following/${userId}`
    );
  }

  getRecommendations(userId: string): Observable<Recommendation[]> {
    return this.http.get<Recommendation[]>(
      `${this.apiBase}/api/followers/${userId}/recommendations`
    );
  }
}
