export interface Problem {
  id: number;
  externalSlug: string;
  title: string;
  leetcodeUrl: string;
  difficultyLabel: string;
  difficultyRating: number | null;
  isCore: boolean;
}

export interface SessionSlot {
  problem: Problem;
  origin: 'novo' | 'revisao' | 'pool';
  urgency: number | null;
}

export interface TodaySession {
  dayNumber: number;
  phase: 'introducao' | 'consolidacao' | 'livre';
  dayType: 'aprendizado' | 'repeticao_pura';
  slots: SessionSlot[];
  reviewBar: SessionSlot[];
  solvedToday: AttemptHistoryItem[];
}

export type AttemptResult =
  | 'GAVE_UP'
  | 'FAILED'
  | 'SOLVED_WITH_HEAVY_HINT'
  | 'SOLVED_WITH_HINT'
  | 'SOLVED'
  | 'SOLVED_FAST_NAMED_PATTERN';

export interface AttemptRequest {
  problemId: number;
  durationSeconds: number;
  result: AttemptResult;
  confidence?: number | null;
  note?: string | null;
}

export interface AttemptResponse {
  attemptId: number;
  problemId: number;
  stabilityDays: number;
  emaQuality: number;
  lastSeenAt: string;
  attemptsCount: number;
  successesCount: number;
}

export interface PatternProgress {
  id: number;
  slug: string;
  name: string;
  orderInPlan: number | null;
  coreTotal: number;
  coreSolved: number;
  avgEmaQuality: number | null;
}

export interface ReviewQueueItem {
  problem: Problem;
  urgency: number;
  stabilityDays: number;
  emaQuality: number;
  lastSeenAt: string;
  attemptsCount: number;
  successesCount: number;
}

export interface StatsResponse {
  totalAttempts: number;
  avgDurationSecondsByDifficulty: Record<string, number>;
  heatmap: { date: string; count: number }[];
}

export interface AttemptHistoryItem {
  attemptId: number;
  problemId: number;
  problemTitle: string;
  problemSlug: string;
  leetcodeUrl: string;
  difficultyLabel: string;
  attemptedAt: string;
  attemptedAtLocalDate: string;
  durationSeconds: number;
  result: AttemptResult;
  confidence: number | null;
  note: string | null;
}

export interface Paper {
  id: number;
  title: string;
  url: string;
  source: string;
  venue: string | null;
  publicationYear: number | null;
  citationCount: number | null;
  authors: string | null;
  cachedAt: string;
}
