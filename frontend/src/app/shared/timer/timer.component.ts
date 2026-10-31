import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

/**
 * Cronômetro que inicia sozinho ao ser montado (ou seja, quando o problema é
 * aberto) e emite o tempo decorrido em segundos quando o usuário termina
 * (docs/spec/original-spec.md seção 7).
 */
@Component({
  selector: 'app-timer',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './timer.component.html',
  styleUrl: './timer.component.scss'
})
export class TimerComponent implements OnInit, OnDestroy {
  /** Instante (ms) em que o problema foi aberto. Permite retomar a contagem depois de recarregar a página. */
  @Input() startedAt = Date.now();
  @Output() finished = new EventEmitter<number>();

  elapsedSeconds = 0;
  private intervalId?: ReturnType<typeof setInterval>;

  ngOnInit(): void {
    this.elapsedSeconds = Math.floor((Date.now() - this.startedAt) / 1000);
    this.intervalId = setInterval(() => {
      this.elapsedSeconds = Math.floor((Date.now() - this.startedAt) / 1000);
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  get formatted(): string {
    const minutes = Math.floor(this.elapsedSeconds / 60)
      .toString()
      .padStart(2, '0');
    const seconds = (this.elapsedSeconds % 60).toString().padStart(2, '0');
    return `${minutes}:${seconds}`;
  }

  finish(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
    this.finished.emit(this.elapsedSeconds);
  }
}
