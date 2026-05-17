import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface Recommendation {
  userId: string;
  username: string;
  mutualCount: number;
}

export interface UserEntry {
  userId: string;
  username: string;
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

  getFollowing(userId: string): Observable<UserEntry[]> {
    return this.http.get<UserEntry[]>(
      `${this.apiBase}/api/followers/${userId}/following`
    );
  }

  getFollowers(userId: string): Observable<UserEntry[]> {
    return this.http.get<UserEntry[]>(
      `${this.apiBase}/api/followers/${userId}/followers`
    );
  }

  /** Fetches username for a userId from stakeholders service */
  getUserInfo(userId: string): Observable<{ id: string; username: string }> {
    return this.http.get<{ id: string; username: string }>(
      `${this.apiBase}/api/users/${userId}`
    ).pipe(catchError(() => of({ id: userId, username: userId })));
  }
}

