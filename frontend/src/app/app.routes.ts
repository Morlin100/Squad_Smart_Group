import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'tarefas' },
  {
    path: 'tarefas',
    loadChildren: () => import('./features/tarefas/tarefas.routes').then((m) => m.TAREFAS_ROUTES),
  },
  { path: '**', redirectTo: 'tarefas' },
];
