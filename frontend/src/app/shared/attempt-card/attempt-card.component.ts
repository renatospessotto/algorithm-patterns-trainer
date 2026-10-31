import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { AttemptsService } from '../../core/services/attempts.service';
import { AttemptHistoryItem, AttemptResponse, AttemptResult, Problem } from '../../core/models';
import { difficultyClass } from '../difficulty';
import { RESULT_OPTIONS, resultClass, resultLabel } from '../attempt-result';
import { TimerComponent } from '../timer/timer.component';
import { ClampedNoteComponent } from '../clamped-note/clamped-note.component';

type Stage = 'idle' | 'timing' | 'form' | 'done';

/**
 * Progresso de um card ainda não registrado, guardado no localStorage. Sem
 * isso, o cronômetro só existia na memória do componente: recarregar a
 * página, trocar de aba no app ou o navegador descartar a aba enquanto você
 * resolvia no LeetCode zerava o card de volta pra "Abrir no LeetCode", e a
 * tentativa nunca chegava ao backend.
 */
interface PendingAttempt {
  startedAt: number;
  elapsedSeconds?: number;
}

const PENDING_KEY_PREFIX = 'pending-attempt:';

/**
 * Card reutilizável de "abrir problema -> cronometrar -> registrar
 * resultado", usado tanto nos slots principais de /hoje quanto na barra de
 * revisão opcional e no modo exploração (/explorar) -- antes disso,
 * /explorar não tinha jeito nenhum de registrar uma tentativa, então um
 * exercício aleatório resolvido lá nunca aparecia em lugar nenhum do app.
 */
@Component({
  selector: 'app-attempt-card',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    TimerComponent,
    ClampedNoteComponent
  ],
  templateUrl: './attempt-card.component.html',
  styleUrl: './attempt-card.component.scss'
})
export class AttemptCardComponent implements OnChanges {
  @Input({ required: true }) problem!: Problem;
  @Input() badgeLabel = '';
  @Input() badgeIcon = '';
  @Input() urgency: number | null = null;
  /** Se o problema já foi resolvido hoje (ex.: reabrindo /hoje depois de já ter feito), mostra direto como "feito". */
  @Input() alreadyDone: AttemptHistoryItem | null = null;
  @Output() recorded = new EventEmitter<AttemptResponse>();

  stage: Stage = 'idle';
  startedAt = 0;
  elapsedSeconds = 0;
  form: FormGroup;
  liveResult?: AttemptResponse;
  saving = false;
  saveError = false;
  readonly resultOptions = RESULT_OPTIONS;

  constructor(
    private attemptsService: AttemptsService,
    private fb: FormBuilder
  ) {
    this.form = this.fb.group({
      minutes: [null, [Validators.required, Validators.min(1)]],
      result: ['SOLVED', Validators.required],
      confidence: [null],
      note: ['']
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['problem'] && !changes['problem'].firstChange) {
      // /explorar reaproveita o mesmo card pra um novo sorteio
      this.stage = 'idle';
      this.liveResult = undefined;
      this.saveError = false;
    }
    if (this.alreadyDone) {
      this.stage = 'done';
      this.clearPending();
      return;
    }
    if (changes['problem'] && this.stage === 'idle') {
      this.restorePending();
    }
  }

  difficultyClass(label: string): string {
    return difficultyClass(label);
  }

  resultLabelFor(result: AttemptResult): string {
    return resultLabel(result);
  }

  resultClassFor(result: AttemptResult): string {
    return resultClass(result);
  }

  open(): void {
    window.open(this.problem.leetcodeUrl, '_blank', 'noopener');
    this.startedAt = Date.now();
    this.stage = 'timing';
    this.savePending({ startedAt: this.startedAt });
  }

  onTimerFinished(seconds: number): void {
    this.savePending({ startedAt: this.startedAt, elapsedSeconds: seconds });
    this.showForm(seconds);
  }

  /** Pra quando você resolveu sem o cronômetro (ou ele se perdeu): vai direto pro formulário e você informa o tempo. */
  registerManually(): void {
    this.showForm(null);
  }

  submit(): void {
    if (this.form.invalid || this.saving) {
      return;
    }
    const { minutes, result, confidence, note } = this.form.value;
    this.saving = true;
    this.saveError = false;
    this.attemptsService
      .record({
        problemId: this.problem.id,
        durationSeconds: Math.max(1, Math.round(minutes * 60)),
        result,
        confidence: confidence || null,
        note: note || null
      })
      .subscribe({
        next: (response) => {
          this.saving = false;
          this.liveResult = response;
          this.stage = 'done';
          this.clearPending();
          this.recorded.emit(response);
        },
        error: () => {
          // Mantém o formulário preenchido pra tentar de novo, em vez de falhar em silêncio.
          this.saving = false;
          this.saveError = true;
        }
      });
  }

  private showForm(seconds: number | null): void {
    this.elapsedSeconds = seconds ?? 0;
    this.form.patchValue({ minutes: seconds === null ? null : Math.max(1, Math.round(seconds / 60)) });
    this.stage = 'form';
  }

  private restorePending(): void {
    const pending = this.readPending();
    if (!pending) {
      return;
    }
    this.startedAt = pending.startedAt;
    if (pending.elapsedSeconds !== undefined) {
      this.showForm(pending.elapsedSeconds);
    } else {
      this.stage = 'timing';
    }
  }

  private get pendingKey(): string {
    return PENDING_KEY_PREFIX + this.problem.id;
  }

  private readPending(): PendingAttempt | null {
    try {
      const raw = localStorage.getItem(this.pendingKey);
      return raw ? (JSON.parse(raw) as PendingAttempt) : null;
    } catch {
      return null;
    }
  }

  private savePending(pending: PendingAttempt): void {
    try {
      localStorage.setItem(this.pendingKey, JSON.stringify(pending));
    } catch {
      // sem storage (aba privada etc.): o card só não sobrevive a reload
    }
  }

  private clearPending(): void {
    try {
      localStorage.removeItem(this.pendingKey);
    } catch {
      // idem
    }
  }
}
