import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

import { StatsService } from '../../core/services/stats.service';
import { PatternsService } from '../../core/services/patterns.service';
import { PatternProgress, StatsResponse } from '../../core/models';

interface HeatmapWeek {
  days: HeatmapDay[];
}

interface HeatmapDay {
  date: string | null;
  count: number;
  level: 0 | 1 | 2 | 3 | 4;
}

const MONTH_LABELS = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];

@Component({
  selector: 'app-stats',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, MatTooltipModule],
  templateUrl: './stats.component.html',
  styleUrl: './stats.component.scss'
})
export class StatsComponent implements OnInit {
  stats?: StatsResponse;
  patterns: PatternProgress[] = [];
  heatmapWeeks: HeatmapWeek[] = [];
  monthLabels: { label: string; weekIndex: number }[] = [];
  currentStreak = 0;
  bestStreak = 0;
  readonly difficultyKeys = ['Easy', 'Medium', 'Hard'];

  constructor(
    private statsService: StatsService,
    private patternsService: PatternsService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.statsService.getStats().subscribe((s) => {
      this.stats = s;
      this.buildHeatmap(s.heatmap);
      this.computeStreaks(s.heatmap);
    });
    this.patternsService.getPatterns().subscribe((p) => (this.patterns = p));
  }

  sortedPatterns(): PatternProgress[] {
    return [...this.patterns].sort((a, b) => (a.avgEmaQuality ?? -1) - (b.avgEmaQuality ?? -1));
  }

  progressPercent(pattern: PatternProgress): number {
    if (pattern.coreTotal === 0) return 0;
    return Math.round((pattern.coreSolved / pattern.coreTotal) * 100);
  }

  strengthClass(pattern: PatternProgress): string {
    if (pattern.avgEmaQuality === null) return 'strength--unknown';
    if (pattern.avgEmaQuality >= 4) return 'strength--strong';
    if (pattern.avgEmaQuality >= 3) return 'strength--medium';
    return 'strength--weak';
  }

  private buildHeatmap(heatmap: StatsResponse['heatmap']): void {
    const countsByDate = new Map(heatmap.map((d) => [d.date, d.count]));
    const maxCount = Math.max(1, ...heatmap.map((d) => d.count));

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const totalDays = 7 * 20; // ~20 semanas de histórico
    const start = new Date(today);
    start.setDate(start.getDate() - totalDays + 1);
    // Alinha ao domingo pra formar colunas de semana certinhas.
    start.setDate(start.getDate() - start.getDay());

    const weeks: HeatmapWeek[] = [];
    const months: { label: string; weekIndex: number }[] = [];
    let cursor = new Date(start);
    let lastMonth = -1;
    let weekIndex = 0;

    while (cursor <= today) {
      const days: HeatmapDay[] = [];
      for (let i = 0; i < 7; i++) {
        if (cursor > today) {
          days.push({ date: null, count: 0, level: 0 });
        } else {
          const iso = cursor.toISOString().slice(0, 10);
          const count = countsByDate.get(iso) ?? 0;
          days.push({ date: iso, count, level: this.levelFor(count, maxCount) });
          if (cursor.getMonth() !== lastMonth) {
            lastMonth = cursor.getMonth();
            months.push({ label: MONTH_LABELS[lastMonth], weekIndex });
          }
        }
        cursor = new Date(cursor);
        cursor.setDate(cursor.getDate() + 1);
      }
      weeks.push({ days });
      weekIndex++;
    }

    this.heatmapWeeks = weeks;
    this.monthLabels = months;
  }

  /** Ponto 3 do pedido do usuário: clicar num dia do heatmap leva pro histórico daquele dia. */
  openDay(day: HeatmapDay): void {
    if (!day.date || day.count === 0) {
      return;
    }
    this.router.navigate(['/historico'], { queryParams: { date: day.date } });
  }

  private levelFor(count: number, max: number): 0 | 1 | 2 | 3 | 4 {
    if (count === 0) return 0;
    const ratio = count / max;
    if (ratio > 0.75) return 4;
    if (ratio > 0.5) return 3;
    if (ratio > 0.25) return 2;
    return 1;
  }

  private computeStreaks(heatmap: StatsResponse['heatmap']): void {
    const daysWithSession = new Set(heatmap.filter((d) => d.count > 0).map((d) => d.date));
    if (daysWithSession.size === 0) {
      this.currentStreak = 0;
      this.bestStreak = 0;
      return;
    }

    let best = 0;
    let running = 0;
    const sortedDates = [...daysWithSession].sort();
    let previous: Date | null = null;
    for (const dateStr of sortedDates) {
      const date = new Date(dateStr + 'T00:00:00');
      if (previous && this.diffInDays(previous, date) === 1) {
        running++;
      } else {
        running = 1;
      }
      best = Math.max(best, running);
      previous = date;
    }
    this.bestStreak = best;

    // Sequência atual: conta pra trás a partir de hoje (ou ontem, se hoje ainda não teve sessão).
    let current = 0;
    const cursor = new Date();
    cursor.setHours(0, 0, 0, 0);
    if (!daysWithSession.has(cursor.toISOString().slice(0, 10))) {
      cursor.setDate(cursor.getDate() - 1);
    }
    while (daysWithSession.has(cursor.toISOString().slice(0, 10))) {
      current++;
      cursor.setDate(cursor.getDate() - 1);
    }
    this.currentStreak = current;
  }

  private diffInDays(a: Date, b: Date): number {
    return Math.round((b.getTime() - a.getTime()) / (1000 * 60 * 60 * 24));
  }
}
