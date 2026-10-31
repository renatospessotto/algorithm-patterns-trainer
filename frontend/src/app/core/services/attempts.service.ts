import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AttemptHistoryItem, AttemptRequest, AttemptResponse } from '../models';

@Injectable({ providedIn: 'root' })
export class AttemptsService {
  constructor(private http: HttpClient) {}

  record(request: AttemptRequest): Observable<AttemptResponse> {
    return this.http.post<AttemptResponse>('/api/attempts', request);
  }

  getHistory(): Observable<AttemptHistoryItem[]> {
    return this.http.get<AttemptHistoryItem[]>('/api/attempts');
  }
}
