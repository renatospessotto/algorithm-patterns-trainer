import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TodaySession } from '../models';

@Injectable({ providedIn: 'root' })
export class TodayService {
  constructor(private http: HttpClient) {}

  getToday(): Observable<TodaySession> {
    return this.http.get<TodaySession>('/api/today');
  }
}
