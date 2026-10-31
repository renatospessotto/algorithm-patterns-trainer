import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { TodayService } from '../../core/services/today.service';
import { AttemptResponse, AttemptHistoryItem, SessionSlot, TodaySession } from '../../core/models';
import { difficultyClass } from '../../shared/difficulty';
import { resultClass, resultLabel } from '../../shared/attempt-result';
import { AttemptCardComponent } from '../../shared/attempt-card/attempt-card.component';
import { ClampedNoteComponent } from '../../shared/clamped-note/clamped-note.component';

@Component({
  selector: 'app-hoje',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, MatProgressSpinnerModule, AttemptCardComponent, ClampedNoteComponent],
  templateUrl: './hoje.component.html',
  styleUrl: './hoje.component.scss'
})
export class HojeComponent implements OnInit {
  session?: TodaySession;
  loading = true;
  error = false;

  constructor(private todayService: TodayService) {}

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading = true;
    this.error = false;
    this.todayService.getToday().subscribe({
      next: (session) => {
        this.session = session;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    });
  }

  /**
   * Depois de registrar qualquer tentativa (slot principal ou barra de
   * revisão), recarrega /today: isso atualiza "resolvidos hoje" e faz a
   * barra de revisão recalcular sem esse problema (já que ele acabou de
   * ser feito). Os 2 slots principais continuam fixos (persistidos no
   * backend), então não saem do lugar.
   */
  onRecorded(_response: AttemptResponse): void {
    this.load();
  }

  /** Recarregar /today não pode recriar os cards, senão o cronômetro de outro slot em andamento se perde. */
  trackByProblem(_index: number, slot: SessionSlot): number {
    return slot.problem.id;
  }

  alreadyDoneFor(problemId: number): AttemptHistoryItem | null {
    return this.session?.solvedToday.find((a) => a.problemId === problemId) ?? null;
  }

  originLabel(origin: SessionSlot['origin']): string {
    switch (origin) {
      case 'novo':
        return 'Problema novo';
      case 'revisao':
        return 'Revisão';
      case 'pool':
        return 'Exploração (ponto fraco)';
    }
  }

  originIcon(origin: SessionSlot['origin']): string {
    switch (origin) {
      case 'novo':
        return 'fiber_new';
      case 'revisao':
        return 'history';
      case 'pool':
        return 'travel_explore';
    }
  }

  difficultyClass(label: string): string {
    return difficultyClass(label);
  }

  resultLabel(result: AttemptHistoryItem['result']): string {
    return resultLabel(result);
  }

  resultClass(result: AttemptHistoryItem['result']): string {
    return resultClass(result);
  }

  formatTime(iso: string): string {
    return new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
  }
}
