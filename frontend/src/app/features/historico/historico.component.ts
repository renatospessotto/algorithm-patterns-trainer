import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';

import { AttemptsService } from '../../core/services/attempts.service';
import { AttemptHistoryItem, AttemptResult } from '../../core/models';
import { difficultyClass } from '../../shared/difficulty';
import { resultClass, resultLabel } from '../../shared/attempt-result';
import { ClampedNoteComponent } from '../../shared/clamped-note/clamped-note.component';

@Component({
  selector: 'app-historico',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatButtonModule,
    ClampedNoteComponent
  ],
  templateUrl: './historico.component.html',
  styleUrl: './historico.component.scss'
})
export class HistoricoComponent implements OnInit {
  history: AttemptHistoryItem[] = [];
  loading = true;
  error = false;
  difficultyFilter: string | null = null;
  /** Filtro por dia exato (YYYY-MM-DD), vindo de ?date= (ex.: clique num dia do heatmap de /stats). */
  dateFilter: string | null = null;

  constructor(
    private attemptsService: AttemptsService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      this.dateFilter = params.get('date');
    });
    this.attemptsService.getHistory().subscribe({
      next: (items) => {
        this.history = items;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    });
  }

  get filtered(): AttemptHistoryItem[] {
    let items = this.history;
    if (this.difficultyFilter) {
      items = items.filter((h) => h.difficultyLabel === this.difficultyFilter);
    }
    if (this.dateFilter) {
      items = items.filter((h) => h.attemptedAtLocalDate === this.dateFilter);
    }
    return items;
  }

  setFilter(label: string | null): void {
    this.difficultyFilter = this.difficultyFilter === label ? null : label;
  }

  clearDateFilter(): void {
    this.router.navigate([], { queryParams: {} });
  }

  resultLabel(result: AttemptResult): string {
    return resultLabel(result);
  }

  difficultyClass(label: string): string {
    return difficultyClass(label);
  }

  resultClass(result: AttemptResult): string {
    return resultClass(result);
  }

  formatDuration(seconds: number): string {
    const minutes = Math.floor(seconds / 60);
    const rest = seconds % 60;
    return `${minutes}m${rest.toString().padStart(2, '0')}s`;
  }
}
