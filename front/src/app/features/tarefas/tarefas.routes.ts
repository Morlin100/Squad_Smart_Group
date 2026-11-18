import { Routes } from '@angular/router';

export const TAREFAS_ROUTES: Routes = [
  {
    path: '',
    title: 'Tarefas',
    loadComponent: () => import('./pages/tarefa-lista/tarefa-lista').then((m) => m.TarefaLista),
  },
  {
    path: 'nova',
    title: 'Nova tarefa',
    loadComponent: () => import('./pages/tarefa-form/tarefa-form').then((m) => m.TarefaForm),
  },
  {
    path: ':id/editar',
    title: 'Editar tarefa',
    loadComponent: () => import('./pages/tarefa-form/tarefa-form').then((m) => m.TarefaForm),
  },
];
