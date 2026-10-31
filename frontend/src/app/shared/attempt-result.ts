import { AttemptResult } from '../core/models';

export const RESULT_OPTIONS: { value: AttemptResult; label: string }[] = [
  { value: 'SOLVED_FAST_NAMED_PATTERN', label: 'Resolvi rápido e nomeei o padrão' },
  { value: 'SOLVED', label: 'Resolvi confortável' },
  { value: 'SOLVED_WITH_HINT', label: 'Resolvi com dica leve' },
  { value: 'SOLVED_WITH_HEAVY_HINT', label: 'Resolvi só com dica pesada / solução' },
  { value: 'FAILED', label: 'Não consegui resolver' },
  { value: 'GAVE_UP', label: 'Desisti' }
];

const RESULT_LABELS: Record<AttemptResult, string> = Object.fromEntries(
  RESULT_OPTIONS.map((o) => [o.value, o.label])
) as Record<AttemptResult, string>;

export function resultLabel(result: AttemptResult): string {
  return RESULT_LABELS[result] ?? result;
}

export function resultClass(result: AttemptResult): string {
  if (result === 'GAVE_UP' || result === 'FAILED') return 'result--bad';
  if (result === 'SOLVED_WITH_HEAVY_HINT' || result === 'SOLVED_WITH_HINT') return 'result--ok';
  return 'result--good';
}
