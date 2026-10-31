import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Problem } from '../models';

@Injectable({ providedIn: 'root' })
export class ProblemsService {
  constructor(private http: HttpClient) {}

  random(pattern?: string, excludeAttempted = true): Observable<Problem | null> {
    const params: Record<string, string> = { exclude_attempted: String(excludeAttempted) };
    if (pattern) {
      params['pattern'] = pattern;
    }
    // Backend devolve 204 sem corpo quando o pool acaba; HttpClient já
    // resolve isso como `null` no body, sem precisar de cast.
    return this.http.get<Problem | null>('/api/problems/random', { params });
  }
}
