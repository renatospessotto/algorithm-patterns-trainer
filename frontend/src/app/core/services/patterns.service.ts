import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PatternProgress, ReviewQueueItem } from '../models';

@Injectable({ providedIn: 'root' })
export class PatternsService {
  constructor(private http: HttpClient) {}

  getPatterns(): Observable<PatternProgress[]> {
    return this.http.get<PatternProgress[]>('/api/patterns');
  }

  getReviewQueue(): Observable<ReviewQueueItem[]> {
    return this.http.get<ReviewQueueItem[]>('/api/queue/review');
  }
}
