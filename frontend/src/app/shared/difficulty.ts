/** Classe CSS pro badge de dificuldade -- compartilhada entre Hoje, Explorar e Histórico. */
export function difficultyClass(label: string): string {
  const normalized = label.toLowerCase();
  if (normalized === 'easy') return 'difficulty--easy';
  if (normalized === 'hard') return 'difficulty--hard';
  return 'difficulty--medium';
}
