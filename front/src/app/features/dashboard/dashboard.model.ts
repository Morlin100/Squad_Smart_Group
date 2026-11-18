import { PrioridadeTarefa, StatusTarefa } from '../tarefas/models/tarefa.model';

export interface Dashboard {
  total: number;
  concluidas: number;
  percentualConcluidas: number;
  vencidas: number;
  criadasUltimos7Dias: number;
  porStatus: Record<StatusTarefa, number>;
  porPrioridade: Record<PrioridadeTarefa, number>;
  porResponsavel: { responsavel: string; quantidade: number }[];
  geradoEm: string;
}

export interface DashboardFiltro {
  responsavel?: string;
  status?: StatusTarefa | '';
  /** yyyy-MM-dd */
  dataInicio?: string | null;
  /** yyyy-MM-dd */
  dataFim?: string | null;
}
