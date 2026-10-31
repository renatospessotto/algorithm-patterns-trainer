import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { StatsResponse } from '../models';

@Injectable({ providedIn: 'root' })
export class StatsService {
  constructor(private http: HttpClient) {}

  getStats(): Observable<StatsResponse> {
    return this.http.get<StatsResponse>('/api/stats');
  }
}
