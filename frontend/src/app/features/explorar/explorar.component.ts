import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { PatternsService } from '../../core/services/patterns.service';
import { ProblemsService } from '../../core/services/problems.service';
import { PatternProgress, Problem } from '../../core/models';
import { AttemptCardComponent } from '../../shared/attempt-card/attempt-card.component';

@Component({
  selector: 'app-explorar',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatCardModule, MatIconModule, MatProgressSpinnerModule, AttemptCardComponent],
  templateUrl: './explorar.component.html',
  styleUrl: './explorar.component.scss'
})
export class ExplorarComponent implements OnInit {
  patterns: PatternProgress[] = [];
  selectedPatternSlug: string | null = null;
  suggestion?: Problem | null;
  searching = false;
  searchedOnce = false;

  constructor(
    private patternsService: PatternsService,
    private problemsService: ProblemsService
  ) {}

  ngOnInit(): void {
    this.patternsService.getPatterns().subscribe((patterns) => (this.patterns = patterns));
  }

  selectPattern(slug: string | null): void {
    this.selectedPatternSlug = this.selectedPatternSlug === slug ? null : slug;
  }

  progressPercent(pattern: PatternProgress): number {
    if (pattern.coreTotal === 0) return 0;
    return Math.round((pattern.coreSolved / pattern.coreTotal) * 100);
  }

  drawRandom(): void {
    this.searching = true;
    this.suggestion = undefined;
    this.problemsService.random(this.selectedPatternSlug ?? undefined).subscribe({
      next: (problem) => {
        this.suggestion = problem;
        this.searching = false;
        this.searchedOnce = true;
      },
      error: () => {
        this.suggestion = null;
        this.searching = false;
        this.searchedOnce = true;
      }
    });
  }

  /**
   * Depois de registrar a tentativa desse aleatório, puxa a atualização de
   * progresso dos padrões (pra refletir na tela) -- o exercício em si já
   * aparece em /hoje ("Resolvidos hoje") e no /historico sozinho, porque os
   * dois só olham pra tabela `attempts`, sem se importar de onde a
   * tentativa veio.
   */
  onRecorded(): void {
    this.patternsService.getPatterns().subscribe((patterns) => (this.patterns = patterns));
  }
}
