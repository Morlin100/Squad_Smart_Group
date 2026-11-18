import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'tarefas' },
  {
    path: 'tarefas',
    loadChildren: () => import('./features/tarefas/tarefas.routes').then((m) => m.TAREFAS_ROUTES),
  },
  {
    path: 'dashboard',
    title: 'Dashboard',
    loadComponent: () => import('./features/dashboard/dashboard-page').then((m) => m.DashboardPage),
  },
  {
    path: 'avaliar',
    title: 'Avaliar o app',
    loadComponent: () => import('./features/pesquisa/pesquisa-page').then((m) => m.PesquisaPage),
  },
  { path: '**', redirectTo: 'tarefas' },
];
