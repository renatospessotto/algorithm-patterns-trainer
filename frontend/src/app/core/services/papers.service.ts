import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Paper } from '../models';

@Injectable({ providedIn: 'root' })
export class PapersService {
  constructor(private http: HttpClient) {}

  getFeed(topic?: string): Observable<Paper[]> {
    const params: Record<string, string> = {};
    if (topic) {
      params['topic'] = topic;
    }
    return this.http.get<Paper[]>('/api/papers/feed', { params });
  }
}
