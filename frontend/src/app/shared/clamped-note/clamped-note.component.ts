import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';

/**
 * Nota de tentativa (o campo livre do formulário) truncada em 2 linhas por
 * padrão, com um botão "Ver mais" pra expandir -- notas longas estavam
 * esticando o card inteiro e desalinhando o grid de /hoje, /explorar e
 * /historico.
 */
@Component({
  selector: 'app-clamped-note',
  standalone: true,
  imports: [CommonModule, MatButtonModule],
  templateUrl: './clamped-note.component.html',
  styleUrl: './clamped-note.component.scss'
})
export class ClampedNoteComponent {
  /** Acima disso, a nota começa clampada com botão "Ver mais". */
  private static readonly COLLAPSE_THRESHOLD = 90;

  @Input({ required: true }) note!: string;

  expanded = false;

  get overflows(): boolean {
    return !!this.note && this.note.length > ClampedNoteComponent.COLLAPSE_THRESHOLD;
  }

  toggle(): void {
    this.expanded = !this.expanded;
  }
}
