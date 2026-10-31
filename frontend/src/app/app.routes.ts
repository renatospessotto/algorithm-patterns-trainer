import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'hoje' },
  {
    path: 'hoje',
    loadComponent: () => import('./features/hoje/hoje.component').then((m) => m.HojeComponent)
  },
  {
    path: 'explorar',
    loadComponent: () => import('./features/explorar/explorar.component').then((m) => m.ExplorarComponent)
  },
  {
    path: 'stats',
    loadComponent: () => import('./features/stats/stats.component').then((m) => m.StatsComponent)
  },
  {
    path: 'leitura',
    loadComponent: () => import('./features/leitura/leitura.component').then((m) => m.LeituraComponent)
  },
  {
    path: 'historico',
    loadComponent: () => import('./features/historico/historico.component').then((m) => m.HistoricoComponent)
  },
  { path: '**', redirectTo: 'hoje' }
];
